"use client"

import { useState, useCallback, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Plus, CheckCircle, XCircle, AlertCircle, Trash2, Search, Edit, ArrowLeft, Loader2, Calculator, FileText } from "lucide-react"
import { getSession, signOut } from "next-auth/react"
import { searchProducts as apiSearchProducts } from "@/lib/api"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../ui/dialog"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { COUNTRY_NAMES } from "./calculate/types"

interface Product {
  code: string
  tlCode?: string
  description?: string
  name?: string
  matchType?: string
}

interface DutyType {
  dutyType: string
  dutyCode: string
  description: string
}

interface NotificationPopup {
  show: boolean
  type: 'success' | 'error' | 'warning'
  title: string
  message: string
  details?: string
}

interface TariffData {
  tariffId: number
  tariffYear: number
  reporterCode: string
  reporterName: string
  partnerCode: string
  partnerName: string
  tlCode: string
  productDescription: string
  dutyType: string
  dutyCode: string
  dutyTypeDescription: string
  dutyCategory?: string
  tlsSuffix?: string
  note?: string
  specificRateUnit?: string
  adValoremRate?: number | null
  specificRate?: number | null
  compoundRate1?: number | null
  compoundRate2?: number | null
  rawText?: string
  isComputable?: boolean
}

interface TariffRequest {
  tariffYear: number
  reporterCode: string
  partnerCode: string
  tlCode: string
  dutyType: string
  dutyCode: string
  tlsSuffix?: string
  note?: string
  specificRateUnit?: string
  adValoremRate?: number
  specificRate?: number
  compoundRate1?: number
  compoundRate2?: number
}

interface DeleteConfirmation {
  show: boolean
  tariffId: number | null
  tariffDetails: string
}

export default function TariffManagementTab() {
  // Tab state
  const [activeTab, setActiveTab] = useState<"search" | "add" | "manage">("search")

  // Search/Edit Form state
  const [selectedProduct, setSelectedProduct] = useState<string>("")
  const [selectedSource, setSelectedSource] = useState<string>("")
  const [selectedDestination, setSelectedDestination] = useState<string>("")
  const [selectedYear, setSelectedYear] = useState<string>("2023")
  const [searchStep, setSearchStep] = useState(1)
  const [searchResults, setSearchResults] = useState<TariffData[]>([])
  
  // Edit dialog state
  const [selectedTariff, setSelectedTariff] = useState<TariffData | null>(null)
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false)
  const [editFormData, setEditFormData] = useState<TariffRequest>({
    tariffYear: new Date().getFullYear(),
    reporterCode: "",
    partnerCode: "",
    tlCode: "",
    dutyType: "",
    dutyCode: "",
    tlsSuffix: "",
    note: "",
    specificRateUnit: "",
  })

  // Add Form state
  const [tariffYear, setTariffYear] = useState<string>(new Date().getFullYear().toString())
  const [reporterCode, setReporterCode] = useState<string>("")
  const [partnerCode, setPartnerCode] = useState<string>("")
  const [productCode, setProductCode] = useState<string>("")
  const [dutyType, setDutyType] = useState<string>("")
  const [dutyCode, setDutyCode] = useState<string>("")
  const [tlsSuffix, setTlsSuffix] = useState<string>("")
  const [note, setNote] = useState<string>("")
  const [specificRateUnit, setSpecificRateUnit] = useState<string>("")
  
  // Duty rates
  const [adValoremRate, setAdValoremRate] = useState<string>("")
  const [specificRate, setSpecificRate] = useState<string>("")
  const [compoundRate1, setCompoundRate1] = useState<string>("")
  const [compoundRate2, setCompoundRate2] = useState<string>("")
  
  // Manage tab state
  const [allTariffs, setAllTariffs] = useState<TariffData[]>([])
  
  // UI state
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  
  // Product search
  const [productSearchQuery, setProductSearchQuery] = useState<string>("")
  const [productSearchResults, setProductSearchResults] = useState<Array<{ code: string, description: string, matchType?: string }>>([])
  const [searchTimeout, setSearchTimeout] = useState<NodeJS.Timeout | null>(null)
  
  // Duty type tracking for dynamic forms
  const [currentDutyType, setCurrentDutyType] = useState<'AD_VALOREM' | 'SPECIFIC' | 'COMBINED' | 'OTHER' | null>(null)
  const [combinedDutyMode, setCombinedDutyMode] = useState<'M' | 'C'>('M')
  
  const [notification, setNotification] = useState<NotificationPopup>({
    show: false,
    type: 'success',
    title: '',
    message: '',
    details: ''
  })

  // Delete confirmation dialog
  const [deleteConfirmation, setDeleteConfirmation] = useState<DeleteConfirmation>({
    show: false,
    tariffId: null,
    tariffDetails: ''
  })

  // Add tariff dialog
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false)

  const predefinedProducts = [
    { code: "27079940", description: "Carbazole, Energy" },
    { code: "1012100", description: "Pure Bred Breeding Horses" },
    { code: "29092000", description: "Cyclanic, Pharmaceutical" },
    { code: "74130000", description: "Copper Wire" }
  ]

  // ✅ Simplified duty type options for users
  const dutyTypes: DutyType[] = [
    { dutyType: "0", dutyCode: "0", description: "Standard (MFN)" },
    { dutyType: "0", dutyCode: "2", description: "Duty-Free" },
    { dutyType: "1", dutyCode: "0", description: "Preferential (Trade Agreement)" },
    { dutyType: "1", dutyCode: "1", description: "Preferential (Specific)" },
    { dutyType: "2", dutyCode: "0", description: "GSP (Developing Countries)" },
    { dutyType: "3", dutyCode: "0", description: "Temporary" },
  ]

  const showNotification = (type: 'success' | 'error' | 'warning', title: string, message: string, details?: string) => {
    setNotification({ show: true, type, title, message, details })
  }

  const hideNotification = () => {
    setNotification(prev => ({ ...prev, show: false }))
  }

  // Normalize backend dutyCategory values to match frontend type system
  const normalizeDutyCategory = (category?: string | null): 'AD_VALOREM' | 'SPECIFIC' | 'COMBINED' | 'OTHER' => {
    if (!category) return 'OTHER'
    const normalized = category.toUpperCase().trim()
    
    if (normalized === 'A') return 'AD_VALOREM'
    if (normalized === 'S') return 'SPECIFIC'
    if (normalized === 'C') return 'COMBINED'
    if (normalized === 'O') return 'OTHER'
    
    if (normalized === 'AD_VALOREM' || normalized === 'AV' || normalized === 'ADVALOREM') return 'AD_VALOREM'
    if (normalized === 'SPECIFIC' || normalized === 'SP') return 'SPECIFIC'
    if (normalized === 'COMBINED' || normalized === 'CO' || normalized === 'COMPOUND') return 'COMBINED'
    
    return 'OTHER'
  }

  // Update current duty type based on which rates user has entered
  // The duty nature (A/S/C/O) is determined by which rates are provided, not from dutyType selection
  useEffect(() => {
    // Determine duty type from entered rates
    const hasAdValorem = adValoremRate && parseFloat(adValoremRate) > 0
    const hasSpecific = specificRate && parseFloat(specificRate) > 0
    const hasCompound1 = compoundRate1 && parseFloat(compoundRate1) > 0
    const hasCompound2 = compoundRate2 && parseFloat(compoundRate2) > 0
    
    if ((hasAdValorem && hasSpecific) || (hasCompound1 && hasCompound2)) {
      setCurrentDutyType('COMBINED')
    } else if (hasAdValorem) {
      setCurrentDutyType('AD_VALOREM')
    } else if (hasSpecific) {
      setCurrentDutyType('SPECIFIC')
    } else {
      // No rates entered - don't set a default
      setCurrentDutyType(null)
    }
  }, [adValoremRate, specificRate, compoundRate1, compoundRate2])

  const searchProducts = useCallback(async (query: string) => {
    try {
      const { ok, data } = await apiSearchProducts(query, 5)
      if (ok && data.products && Array.isArray(data.products)) {
        return data.products.map((p: Product) => ({
          code: p.code || p.tlCode,
          description: p.description || p.name || "No description available",
          matchType: p.matchType
        }))
      }
      const isNumericQuery = /^\d+$/.test(query)
      const filtered = predefinedProducts.filter(product =>
        product.code.toLowerCase().includes(query.toLowerCase()) ||
        product.description.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5)
      return filtered.map(product => ({
        ...product,
        matchType: isNumericQuery && product.code.includes(query) ? 'contains_code' : 'description_match'
      }))
    } catch (error) {
      console.error('Product search error:', error)
      const isNumericQuery = /^\d+$/.test(query)
      const filtered = predefinedProducts.filter(product =>
        product.code.toLowerCase().includes(query.toLowerCase()) ||
        product.description.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5)
      return filtered.map(product => ({
        ...product,
        matchType: isNumericQuery && product.code.includes(query) ? 'contains_code' : 'description_match'
      }))
    }
  }, [])

  useEffect(() => {
    if (searchTimeout) clearTimeout(searchTimeout)
    if (productSearchQuery.length > 0) {
      const timeout = setTimeout(async () => {
        const results = await searchProducts(productSearchQuery)
        setProductSearchResults(results)
      }, 300)
      setSearchTimeout(timeout)
    } else {
      setProductSearchResults([])
    }
    return () => { if (searchTimeout) clearTimeout(searchTimeout) }
  }, [productSearchQuery, searchProducts])

  const handleProductSelect = (product: { code: string, description: string }) => {
    if (activeTab === "search") {
      setSelectedProduct(product.code)
    } else {
      setProductCode(product.code)
    }
    setProductSearchQuery(`${product.code} - ${product.description}`)
    setProductSearchResults([])
  }

  const handleProductSearchChange = (value: string) => {
    setProductSearchQuery(value)
    
    const trimmedValue = value.trim()
    if (/^\d+$/.test(trimmedValue) && trimmedValue.length >= 4) {
      if (activeTab === "search") {
        setSelectedProduct(trimmedValue)
      } else {
        setProductCode(trimmedValue)
      }
    } else if (trimmedValue === "") {
      if (activeTab === "search") {
        setSelectedProduct("")
      } else {
        setProductCode("")
      }
    }
  }

  const getAuthHeaders = useCallback(async (): Promise<HeadersInit> => {
    if (typeof window === "undefined") return {}
    try {
      const session = await getSession()
      if (session?.error === "RefreshAccessTokenError") {
        await signOut({ callbackUrl: '/login' })
        throw new Error("Session expired. Please sign in again.")
      }
      const token = session?.accessToken
      return {
        "Content-Type": "application/json",
        "Authorization": token ? `Bearer ${token}` : ""
      }
    } catch (error) {
      console.error("❌ Error getting auth headers:", error)
      throw error
    }
  }, [])

  const resetAddForm = () => {
    setTariffYear(new Date().getFullYear().toString())
    setReporterCode("")
    setPartnerCode("")
    setProductCode("")
    setProductSearchQuery("")
    setDutyType("")
    setDutyCode("")
    setTlsSuffix("")
    setNote("")
    setSpecificRateUnit("")
    setAdValoremRate("")
    setSpecificRate("")
    setCompoundRate1("")
    setCompoundRate2("")
    setCurrentDutyType(null)
    setCombinedDutyMode('M')
  }

  // SEARCH & EDIT FUNCTIONS
  const handleSearchTariffs = async () => {
    setLoading(true)
    setError("")
    setSuccess("")

    try {
      const headers = await getAuthHeaders()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

      const searchRequest = {
        reporterCode: selectedDestination,
        partnerCode: selectedSource,
        productCode: selectedProduct,
        year: parseInt(selectedYear),
      }

      console.log("🔍 Searching tariffs with:", searchRequest)

      const response = await fetch(`${apiUrl}/api/admin/tariffs/search`, {
        method: "POST",
        headers,
        body: JSON.stringify(searchRequest),
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`Search failed: ${errorText}`)
      }

      const data = await response.json()
      console.log("📦 Received search results:", data)

      const mappedResults: TariffData[] = (data.tariffs || []).map((tariff: Record<string, unknown>) => ({
        tariffId: tariff.tariffId,
        tariffYear: tariff.tariffYear || tariff.year,
        reporterCode: tariff.reporterCode,
        reporterName: tariff.reporterName,
        partnerCode: tariff.partnerCode,
        partnerName: tariff.partnerName,
        tlCode: tariff.tlCode,
        productDescription: tariff.productDescription,
        dutyType: tariff.dutyType,
        dutyCode: tariff.dutyCode,
        dutyTypeDescription: tariff.dutyTypeDescription,
        dutyCategory: tariff.dutyCategory,
        tlsSuffix: tariff.tlsSuffix || "",
        note: tariff.note || "",
        specificRateUnit: tariff.specificRateUnit || "",
        adValoremRate: tariff.adValoremRate ?? null,
        specificRate: tariff.specificRate ?? null,
        compoundRate1: tariff.compoundRate1 ?? null,
        compoundRate2: tariff.compoundRate2 ?? null,
        rawText: tariff.rawText,
        isComputable: tariff.isComputable,
      }))

      console.log("✅ Mapped results:", mappedResults)

      setSearchResults(mappedResults)
      setSearchStep(2)

      if (mappedResults.length === 0) {
        setError("No tariffs found matching your criteria")
      } else {
        setSuccess(`Found ${mappedResults.length} tariff(s)`)
      }
    } catch (e) {
      const err = e as Error
      console.error("❌ Search error:", err)
      setError(`Search failed: ${err.message}`)
    } finally {
      setLoading(false)
    }
  }

  const handleEditTariff = async (tariff: TariffData) => {
    console.log("✏️ Opening edit dialog for tariff:", tariff.tariffId)
    
    if (!tariff.reporterCode || !tariff.partnerCode || !tariff.tlCode || !tariff.dutyType || !tariff.dutyCode) {
      console.error("❌ Missing required fields in tariff:", tariff)
      setError("Error: Tariff data is incomplete")
      return
    }
    
    const formData: TariffRequest = {
      tariffYear: tariff.tariffYear,
      reporterCode: tariff.reporterCode,
      partnerCode: tariff.partnerCode,
      tlCode: tariff.tlCode,
      dutyType: tariff.dutyType,
      dutyCode: tariff.dutyCode,
      tlsSuffix: tariff.tlsSuffix || "",
      note: tariff.note || "",
      specificRateUnit: tariff.specificRateUnit || "",
      ...(tariff.adValoremRate !== undefined && tariff.adValoremRate !== null && { adValoremRate: tariff.adValoremRate }),
      ...(tariff.specificRate !== undefined && tariff.specificRate !== null && { specificRate: tariff.specificRate }),
      ...(tariff.compoundRate1 !== undefined && tariff.compoundRate1 !== null && { compoundRate1: tariff.compoundRate1 }),
      ...(tariff.compoundRate2 !== undefined && tariff.compoundRate2 !== null && { compoundRate2: tariff.compoundRate2 }),
    }
    
    let detectedType: 'AD_VALOREM' | 'SPECIFIC' | 'COMBINED' | 'OTHER' = 'OTHER'
    
    if (tariff.dutyCategory) {
      detectedType = normalizeDutyCategory(tariff.dutyCategory)
    } else {
      const hasAdValorem = (tariff.adValoremRate !== undefined && tariff.adValoremRate !== null && tariff.adValoremRate > 0)
      const hasSpecific = (tariff.specificRate !== undefined && tariff.specificRate !== null && tariff.specificRate > 0)
      const hasCompound1 = (tariff.compoundRate1 !== undefined && tariff.compoundRate1 !== null && tariff.compoundRate1 > 0)
      const hasCompound2 = (tariff.compoundRate2 !== undefined && tariff.compoundRate2 !== null && tariff.compoundRate2 > 0)
      
      if ((hasAdValorem && hasSpecific) || (hasCompound1 && hasCompound2)) {
        detectedType = 'COMBINED'
      } else if (hasAdValorem && !hasSpecific && !hasCompound1 && !hasCompound2) {
        detectedType = 'AD_VALOREM'
      } else if (hasSpecific && !hasAdValorem && !hasCompound1 && !hasCompound2) {
        detectedType = 'SPECIFIC'
      } else {
        detectedType = 'OTHER'
      }
    }
    
    setCurrentDutyType(detectedType)
    setSelectedTariff(tariff)
    setEditFormData(formData)
    setIsEditDialogOpen(true)
  }

  const handleSaveTariff = async () => {
    if (!selectedTariff) {
      console.error("❌ selectedTariff is null!")
      setError("Error: No tariff selected")
      return
    }
    
    setLoading(true)
    setError("")
    setSuccess("")
    
    try {
      const headers = await getAuthHeaders()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
      const endpoint = `${apiUrl}/api/admin/tariffs/${selectedTariff.tariffId}`
      
      const requestBody: Record<string, unknown> = {
        tlsSuffix: editFormData.tlsSuffix || selectedTariff.tlsSuffix || "",
        note: editFormData.note || selectedTariff.note || "",
        specificRateUnit: editFormData.specificRateUnit || selectedTariff.specificRateUnit || "",
      }
      
      const adValoremValue = editFormData.adValoremRate !== undefined 
        ? Number(editFormData.adValoremRate)
        : (selectedTariff.adValoremRate !== undefined ? Number(selectedTariff.adValoremRate) : undefined)
    
      if (adValoremValue !== undefined && !isNaN(adValoremValue) && adValoremValue >= 0) {
        requestBody.adValoremRate = adValoremValue
      }
    
      const specificValue = editFormData.specificRate !== undefined 
        ? Number(editFormData.specificRate)
        : (selectedTariff.specificRate !== undefined ? Number(selectedTariff.specificRate) : undefined)
    
      if (specificValue !== undefined && !isNaN(specificValue) && specificValue >= 0) {
        requestBody.specificRate = specificValue
      }
    
      const compound1Value = editFormData.compoundRate1 !== undefined 
        ? Number(editFormData.compoundRate1)
        : (selectedTariff.compoundRate1 !== undefined ? Number(selectedTariff.compoundRate1) : undefined)
    
      if (compound1Value !== undefined && !isNaN(compound1Value) && compound1Value >= 0) {
        requestBody.compoundRate1 = compound1Value
      }
    
      const compound2Value = editFormData.compoundRate2 !== undefined 
        ? Number(editFormData.compoundRate2)
        : (selectedTariff.compoundRate2 !== undefined ? Number(selectedTariff.compoundRate2) : undefined)
    
      if (compound2Value !== undefined && !isNaN(compound2Value) && compound2Value >= 0) {
        requestBody.compoundRate2 = compound2Value
      }
      
      const response = await fetch(endpoint, {
        method: 'PUT',
        headers,
        body: JSON.stringify(requestBody),
        mode: 'cors',
        credentials: 'include',
      })
      
      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`Update failed (${response.status}): ${errorText}`)
      }
      
      const updatedTariff = await response.json()
      console.log("✅ Tariff updated:", updatedTariff)
      
      setSuccess("Tariff updated successfully!")
      setIsEditDialogOpen(false)
      setSelectedTariff(null)
      
      showNotification(
        'success',
        'Tariff Updated! 🎉',
        `Tariff ID ${selectedTariff.tariffId} has been successfully updated`,
        `Product: ${selectedTariff.tlCode}\nRoute: ${selectedTariff.partnerCode} → ${selectedTariff.reporterCode}\nYear: ${selectedTariff.tariffYear}`
      )
      
      await handleSearchTariffs()
      
    } catch (e) {
      const err = e as Error
      console.error("❌ Update failed:", err)
      setError(`Update failed: ${err.message}`)
      showNotification('error', 'Update Failed ❌', `Failed to update tariff ${selectedTariff.tariffId}`, err.message)
    } finally {
      setLoading(false)
    }
  }

  // ADD TARIFF FUNCTIONS
  const handleCreateTariff = async () => {
    setLoading(true)
    setError("")
    setSuccess("")

    try {
      const missingFields = []
      if (!tariffYear) missingFields.push("Tariff Year")
      if (!reporterCode) missingFields.push("Reporter Country")
      if (!partnerCode) missingFields.push("Partner Country")
      if (!productCode) missingFields.push("Product")
      if (!dutyType) missingFields.push("Duty Type")
      if (!dutyCode) missingFields.push("Duty Code")
      
      if (missingFields.length > 0) {
        throw new Error(`Please fill in the following required fields: ${missingFields.join(", ")}`)
      }

      const hasAdValorem = adValoremRate && adValoremRate.trim() !== "" && parseFloat(adValoremRate) >= 0
      const hasSpecific = specificRate && specificRate.trim() !== "" && parseFloat(specificRate) >= 0
      const hasCompound1 = compoundRate1 && compoundRate1.trim() !== "" && parseFloat(compoundRate1) >= 0
      const hasCompound2 = compoundRate2 && compoundRate2.trim() !== "" && parseFloat(compoundRate2) >= 0
      
      if (!hasAdValorem && !hasSpecific && !hasCompound1 && !hasCompound2) {
        throw new Error("At least one duty rate must be specified (Ad Valorem, Specific, or Combined rates)")
      }
      
      if ((hasSpecific || hasCompound2) && (!specificRateUnit || specificRateUnit.trim() === "")) {
        throw new Error("Specific rate unit is required when using specific or combined duties")
      }

      const headers = await getAuthHeaders()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

      const requestBody: Record<string, unknown> = {
        tariffYear: parseInt(tariffYear),
        reporterCode: reporterCode,
        partnerCode: partnerCode,
        tlCode: productCode,
        dutyType: dutyType,
        dutyCode: dutyCode,
        tlsSuffix: tlsSuffix || "",
        note: note || "",
        specificRateUnit: specificRateUnit || "",
      }

      if (adValoremRate && adValoremRate.trim() !== "") {
        const value = parseFloat(adValoremRate)
        if (!isNaN(value) && value >= 0) {
          requestBody.adValoremRate = value
        }
      }
      
      if (specificRate && specificRate.trim() !== "") {
        const value = parseFloat(specificRate)
        if (!isNaN(value) && value >= 0) {
          requestBody.specificRate = value
        }
      }
      
      if (compoundRate1 && compoundRate1.trim() !== "") {
        const value = parseFloat(compoundRate1)
        if (!isNaN(value) && value >= 0) {
          requestBody.compoundRate1 = value
        }
      }
      
      if (compoundRate2 && compoundRate2.trim() !== "") {
        const value = parseFloat(compoundRate2)
        if (!isNaN(value) && value >= 0) {
          requestBody.compoundRate2 = value
        }
      }

      const response = await fetch(`${apiUrl}/api/admin/tariffs`, {
        method: 'POST',
        headers,
        body: JSON.stringify(requestBody),
        mode: 'cors',
        credentials: 'include',
      })

      if (!response.ok) {
        let errorMessage = `Failed to create tariff (${response.status})`
        try {
          const errorText = await response.text()
          try {
            const errorJson = JSON.parse(errorText)
            errorMessage = errorJson.message || errorJson.error || errorText || errorMessage
          } catch {
            errorMessage = errorText || errorMessage
          }
        } catch (parseError) {
          console.error("❌ Could not parse error response:", parseError)
        }
        throw new Error(errorMessage)
      }

      const createdTariff = await response.json()
      console.log("✅ Tariff created:", createdTariff)

      setSuccess(`Tariff created successfully! ID: ${createdTariff.tariffId}`)
      
      showNotification(
        'success',
        'Tariff Created! 🎉',
        `New tariff has been added to the database`,
        `Tariff ID: ${createdTariff.tariffId}\nProduct: ${productCode}\nYear: ${tariffYear}\nReporter: ${reporterCode} → Partner: ${partnerCode}`
      )

      setIsAddDialogOpen(false)
      setTimeout(() => {
        resetAddForm()
      }, 500)

    } catch (e) {
      const err = e as Error
      console.error("❌ Create failed:", err)
      setError(err.message)
      showNotification('error', 'Creation Failed ❌', 'Failed to create tariff', err.message)
    } finally {
      setLoading(false)
    }
  }

  // MANAGE (VIEW ALL) FUNCTIONS
  const handleLoadAllTariffs = async () => {
    setLoading(true)
    setError("")
    setSuccess("")
    
    try {
      const headers = await getAuthHeaders()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
      
      const response = await fetch(`${apiUrl}/api/admin/tariffs`, {
        method: 'GET',
        headers,
        mode: 'cors',
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error(`Failed to fetch tariffs: ${response.status}`)
      }

      const data = await response.json()
      const tariffs = data.tariffs || []
      
      setAllTariffs(tariffs)
      setSuccess(`Found ${tariffs.length} tariff(s)`)
    } catch (e) {
      const err = e as Error
      console.error("❌ Load failed:", err)
      setError(`Failed to load tariffs: ${err.message}`)
    } finally {
      setLoading(false)
    }
  }

  // DELETE FUNCTIONS
  const handleDeleteTariff = async (tariffId: number) => {
    setLoading(true)
    
    try {
      const headers = await getAuthHeaders()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
      
      const response = await fetch(`${apiUrl}/api/admin/tariffs/${tariffId}`, {
        method: 'DELETE',
        headers,
        mode: 'cors',
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error(`Delete failed (${response.status})`)
      }

      setSuccess("Tariff deleted successfully!")
      
      showNotification(
        'success',
        'Tariff Deleted! 🗑️',
        `Tariff ID ${tariffId} has been permanently removed from the database`,
        'This action cannot be undone.'
      )
      
      setDeleteConfirmation({ show: false, tariffId: null, tariffDetails: '' })
      
      if (activeTab === "manage") {
        await handleLoadAllTariffs()
      } else {
        await handleSearchTariffs()
      }
      
    } catch (e) {
      const err = e as Error
      console.error("❌ Delete failed:", err)
      setError(`Delete failed: ${err.message}`)
      showNotification('error', 'Delete Failed ❌', `Failed to delete tariff ${tariffId}`, err.message)
    } finally {
      setLoading(false)
    }
  }

  const confirmDelete = (tariff: TariffData) => {
    const details = `Tariff ID: ${tariff.tariffId}
Product: ${tariff.tlCode} - ${tariff.productDescription}
Route: ${tariff.partnerName} → ${tariff.reporterName}
Year: ${tariff.tariffYear}
Duty Category: ${tariff.dutyCategory || 'Unknown'}`
    
    setDeleteConfirmation({
      show: true,
      tariffId: tariff.tariffId,
      tariffDetails: details
    })
  }

  const cancelDelete = () => {
    setDeleteConfirmation({ show: false, tariffId: null, tariffDetails: '' })
  }

  return (
    <div className="h-full flex flex-col space-y-3 p-1">
      <Card className="flex-1 bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700">
        <CardHeader className="pb-4 border-b border-slate-200 dark:border-slate-700">
          <CardTitle className="text-lg flex items-center gap-2 text-slate-900 dark:text-slate-100">
            <Edit className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            Tariff Management (Admin Only)
          </CardTitle>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            Search, edit, add, and manage tariff entries
          </p>
        </CardHeader>
        
        <CardContent className="pt-4">
          <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as "search" | "add" | "manage")}>
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="search">Search & Edit</TabsTrigger>
              <TabsTrigger value="add">Add New</TabsTrigger>
              <TabsTrigger value="manage">View All</TabsTrigger>
            </TabsList>
            
            {/* SEARCH & EDIT TAB */}
            <TabsContent value="search" className="space-y-4">
              {searchStep === 1 ? (
                <div className="space-y-3">
                  <div className="space-y-1.5">
                    <Label htmlFor="source" className="text-sm font-medium">Source Country (Partner)</Label>
                    <Select onValueChange={setSelectedSource}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select source" />
                      </SelectTrigger>
                      <SelectContent className="max-h-60 overflow-y-auto">
                        {Object.entries(COUNTRY_NAMES).map(([code, name]) => (
                          <SelectItem key={code} value={code}>
                            {code} - {name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="destination" className="text-sm font-medium">Destination Country (Reporter)</Label>
                    <Select onValueChange={setSelectedDestination}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select destination" />
                      </SelectTrigger>
                      <SelectContent className="max-h-60 overflow-y-auto">
                        {Object.entries(COUNTRY_NAMES)
                          .filter(([code]) => code !== "000") // Exclude "World" from reporter
                          .map(([code, name]) => (
                            <SelectItem key={code} value={code}>
                              {code} - {name}
                            </SelectItem>
                          ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="product" className="text-sm font-medium">Product Search</Label>
                    <div className="relative">
                      <Input
                        type="text"
                        value={productSearchQuery}
                        onChange={(e) => handleProductSearchChange(e.target.value)}
                        placeholder="Search by HS Code or description"
                      />
                      {productSearchResults.length > 0 && productSearchQuery && (
                        <div className="absolute z-10 w-full mt-1 bg-white dark:bg-slate-800 border rounded-lg shadow-lg">
                          {productSearchResults.map((product, idx) => (
                            <div
                              key={idx}
                              onClick={() => handleProductSelect(product)}
                              className="p-2 hover:bg-slate-100 dark:hover:bg-slate-700 cursor-pointer"
                            >
                              <div className="font-semibold">{product.code}</div>
                              <div className="text-sm text-slate-600 dark:text-slate-400">{product.description}</div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="year" className="text-sm font-medium">Year</Label>
                    <Select onValueChange={setSelectedYear} value={selectedYear}>
                      <SelectTrigger>
                        <SelectValue placeholder="Select year" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="2023">2023</SelectItem>
                        <SelectItem value="2024">2024</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <Button
                    onClick={handleSearchTariffs}
                    disabled={loading || !selectedProduct || !selectedSource || !selectedDestination}
                    className="w-full bg-blue-600 hover:bg-blue-700"
                  >
                    <Search className="h-4 w-4 mr-2" />
                    {loading ? "Searching..." : "Search Tariffs"}
                  </Button>
                </div>
              ) : (
                <div className="space-y-3">
                  <Button variant="outline" onClick={() => setSearchStep(1)} className="w-full">
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Back to Search
                  </Button>

                  <div className="space-y-3 max-h-96 overflow-y-auto">
                    {searchResults.map((tariff) => (
                      <Card key={tariff.tariffId} className="border-2">
                        <CardContent className="p-4">
                          <div className="flex justify-between items-start">
                            <div className="flex-1">
                              <div className="font-bold text-lg">ID: {tariff.tariffId}</div>
                              <div className="text-sm">Product: {tariff.tlCode} - {tariff.productDescription}</div>
                              <div className="text-sm">Route: {tariff.partnerName} → {tariff.reporterName}</div>
                              <div className="text-sm">Year: {tariff.tariffYear}</div>
                            </div>
                            <div className="flex gap-2">
                              <Button size="sm" onClick={() => handleEditTariff(tariff)}>
                                <Edit className="h-4 w-4" />
                              </Button>
                              <Button size="sm" variant="destructive" onClick={() => confirmDelete(tariff)}>
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </div>
              )}
            </TabsContent>

            {/* ADD NEW TAB */}
            <TabsContent value="add" className="space-y-4">
              <Button
                onClick={() => setIsAddDialogOpen(true)}
                className="w-full bg-green-600 hover:bg-green-700"
              >
                <Plus className="h-4 w-4 mr-2" />
                Create New Tariff
              </Button>
            </TabsContent>

            {/* VIEW ALL TAB */}
            <TabsContent value="manage" className="space-y-4">
              <Button
                onClick={handleLoadAllTariffs}
                disabled={loading}
                className="w-full bg-blue-600 hover:bg-blue-700"
              >
                {loading ? "Loading..." : "Load All Tariffs"}
              </Button>

              {allTariffs.length > 0 && (
                <div className="space-y-3 max-h-96 overflow-y-auto">
                  {allTariffs.map((tariff) => (
                    <Card key={tariff.tariffId} className="border-2">
                      <CardContent className="p-4">
                        <div className="flex justify-between items-start">
                          <div className="flex-1">
                            <div className="font-bold text-lg">ID: {tariff.tariffId}</div>
                            <div className="text-sm">Product: {tariff.tlCode} - {tariff.productDescription}</div>
                            <div className="text-sm">Route: {tariff.partnerName} → {tariff.reporterName}</div>
                            <div className="text-sm">Year: {tariff.tariffYear}</div>
                          </div>
                          <div className="flex gap-2">
                            <Button size="sm" variant="destructive" onClick={() => confirmDelete(tariff)}>
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              )}
            </TabsContent>
          </Tabs>
        </CardContent>
      </Card>

      {/* Add Tariff Dialog - Full Featured from TariffsTab */}
      <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto bg-white dark:bg-slate-900 border-2 border-slate-300 dark:border-slate-700">
          <DialogHeader className="pb-4 border-b border-slate-200 dark:border-slate-700">
            <DialogTitle className="text-2xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <Plus className="h-6 w-6 text-green-600 dark:text-green-400" />
              Add New Tariff
            </DialogTitle>
            <DialogDescription className="text-slate-600 dark:text-slate-400 mt-2">
              Create a new tariff entry in the database. All fields marked with <span className="text-red-500 font-semibold">*</span> are required.
              <br />
              <span className="text-blue-600 dark:text-blue-400 font-medium">💡 Tip: You can enter any HS code - new products will be auto-created. Duty types will also be auto-created if needed.</span>
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-6 py-4">
            {/* Required Fields Section */}
            <div className="space-y-3">
              <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-red-500" />
                Required Information
              </h3>
              
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Tariff Year <span className="text-red-500">*</span>
                  </Label>
                  <Input 
                    type="number"
                    value={tariffYear} 
                    onChange={(e) => setTariffYear(e.target.value)}
                    placeholder="2023"
                    className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                  />
                </div>

                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Reporter Country (Destination) <span className="text-red-500">*</span>
                  </Label>
                  <Select onValueChange={setReporterCode}>
                    <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600">
                      <SelectValue placeholder="Select reporter" />
                    </SelectTrigger>
                    <SelectContent className="max-h-60 overflow-y-auto">
                      {Object.entries(COUNTRY_NAMES)
                        .filter(([code]) => code !== "000") // Exclude "World" from reporter
                        .map(([code, name]) => (
                          <SelectItem key={code} value={code}>
                            {code} - {name}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Partner Country (Source) <span className="text-red-500">*</span>
                  </Label>
                  <Select onValueChange={setPartnerCode}>
                    <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600">
                      <SelectValue placeholder="Select partner" />
                    </SelectTrigger>
                    <SelectContent className="max-h-60 overflow-y-auto">
                      {Object.entries(COUNTRY_NAMES).map(([code, name]) => (
                        <SelectItem key={code} value={code}>
                          {code} - {name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Product (HS Code) <span className="text-red-500">*</span>
                  </Label>
                  <div className="relative">
                    <Input
                      value={productSearchQuery}
                      onChange={(e) => handleProductSearchChange(e.target.value)}
                      placeholder="Search by HS Code or description..."
                      className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                    />
                    {productSearchResults.length > 0 && productSearchQuery && (
                      <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border-2 border-slate-300 dark:border-slate-600 rounded-lg shadow-xl max-h-60 overflow-y-auto">
                        {productSearchResults.map((product, idx) => (
                          <div
                            key={idx}
                            onClick={() => handleProductSelect(product)}
                            className="p-3 hover:bg-blue-50 dark:hover:bg-blue-900/20 cursor-pointer border-b last:border-b-0 transition-colors"
                          >
                            <div className="font-bold text-blue-600 dark:text-blue-400">{product.code}</div>
                            <div className="text-sm text-slate-600 dark:text-slate-400">{product.description}</div>
                            {product.matchType && (
                              <div className="text-xs text-green-600 dark:text-green-400 mt-1">
                                {product.matchType === 'contains_code' ? '✓ Code Match' : '✓ Description Match'}
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  {productCode && (
                    <div className="text-xs text-green-600 dark:text-green-400 font-medium mt-1">
                      ✓ Selected: {productCode}
                    </div>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Duty Type <span className="text-red-500">*</span>
                  </Label>
                  <Select onValueChange={(value) => {
                    const selected = dutyTypes.find(d => `${d.dutyType}-${d.dutyCode}` === value)
                    if (selected) {
                      setDutyType(selected.dutyType)
                      setDutyCode(selected.dutyCode)
                    }
                  }}>
                    <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600">
                      <SelectValue placeholder="Select duty type" />
                    </SelectTrigger>
                    <SelectContent>
                      {dutyTypes.map((dt) => (
                        <SelectItem key={`${dt.dutyType}-${dt.dutyCode}`} value={`${dt.dutyType}-${dt.dutyCode}`}>
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-xs bg-slate-100 dark:bg-slate-700 px-2 py-0.5 rounded">
                              {dt.dutyType}-{dt.dutyCode}
                            </span>
                            <span>{dt.description}</span>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {dutyType && dutyCode && (
                    <div className="text-xs text-green-600 dark:text-green-400 font-medium mt-1">
                      ✓ Selected: {dutyType}-{dutyCode}
                    </div>
                  )}
                </div>

                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Specific Rate Unit
                  </Label>
                  <Input 
                    value={specificRateUnit} 
                    onChange={(e) => setSpecificRateUnit(e.target.value)}
                    placeholder="e.g., kg, liter, piece"
                    className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Required for specific/compound duties
                  </p>
                </div>
              </div>
            </div>

            {/* Detected Duty Category Badge */}
            {currentDutyType && (
              <div className={`p-4 rounded-lg border-2 ${
                currentDutyType === 'AD_VALOREM' 
                  ? 'bg-blue-50 dark:bg-blue-900/20 border-blue-300 dark:border-blue-700'
                  : currentDutyType === 'SPECIFIC'
                  ? 'bg-purple-50 dark:bg-purple-900/20 border-purple-300 dark:border-purple-700'
                  : currentDutyType === 'COMBINED'
                  ? 'bg-orange-50 dark:bg-orange-900/20 border-orange-300 dark:border-orange-700'
                  : 'bg-gray-50 dark:bg-gray-900/20 border-gray-300 dark:border-gray-700'
              }`}>
                <div className="flex items-center gap-3">
                  <div className={`w-3 h-3 rounded-full ${
                    currentDutyType === 'AD_VALOREM' ? 'bg-blue-500' :
                    currentDutyType === 'SPECIFIC' ? 'bg-purple-500' :
                    currentDutyType === 'COMBINED' ? 'bg-orange-500' : 'bg-gray-500'
                  }`} />
                  <div>
                    <div className="font-bold text-sm text-slate-900 dark:text-slate-100">
                      Detected Duty Category: {
                        currentDutyType === 'AD_VALOREM' ? '📊 Ad Valorem (Percentage-based)' :
                        currentDutyType === 'SPECIFIC' ? '⚖️ Specific (Weight/Volume-based)' :
                        currentDutyType === 'COMBINED' ? '🔀 Combined (Mixed Rates)' : '❓ Other'
                      }
                    </div>
                    <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">
                      {currentDutyType === 'AD_VALOREM' && 'Uses percentage of product value'}
                      {currentDutyType === 'SPECIFIC' && 'Uses fixed rate per unit (weight, volume, etc.)'}
                      {currentDutyType === 'COMBINED' && 'Uses multiple rate types (e.g., 10% + $2/kg)'}
                      {currentDutyType === 'OTHER' && 'Custom or special duty structure'}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Duty Rates Section */}
            <div className="space-y-3">
              <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
                <Calculator className="h-4 w-4 text-blue-500" />
                Duty Rates <span className="text-xs font-normal text-slate-500">(At least one required)</span>
              </h3>
              
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                    Ad Valorem Rate (%)
                    {currentDutyType === 'AD_VALOREM' && <span className="text-xs bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded-full">Primary</span>}
                  </Label>
                  <Input 
                    type="number" 
                    step="0.01"
                    min="0"
                    value={adValoremRate} 
                    onChange={(e) => setAdValoremRate(e.target.value)}
                    placeholder="0.00"
                    className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Percentage of product value (e.g., 5.5 for 5.5%)
                  </p>
                </div>

                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                    Specific Rate
                    {currentDutyType === 'SPECIFIC' && <span className="text-xs bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300 px-2 py-0.5 rounded-full">Primary</span>}
                  </Label>
                  <Input 
                    type="number" 
                    step="0.01"
                    min="0"
                    value={specificRate} 
                    onChange={(e) => setSpecificRate(e.target.value)}
                    placeholder="0.00"
                    className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Fixed rate per unit (requires unit above)
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                    Compound Rate 1
                    {currentDutyType === 'COMBINED' && <span className="text-xs bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300 px-2 py-0.5 rounded-full">Primary</span>}
                  </Label>
                  <Input 
                    type="number" 
                    step="0.01"
                    min="0"
                    value={compoundRate1} 
                    onChange={(e) => setCompoundRate1(e.target.value)}
                    placeholder="0.00"
                    className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    First component of combined duty
                  </p>
                </div>

                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-2">
                    Compound Rate 2
                    {currentDutyType === 'COMBINED' && <span className="text-xs bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300 px-2 py-0.5 rounded-full">Primary</span>}
                  </Label>
                  <Input 
                    type="number" 
                    step="0.01"
                    min="0"
                    value={compoundRate2} 
                    onChange={(e) => setCompoundRate2(e.target.value)}
                    placeholder="0.00"
                    className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Second component of combined duty
                  </p>
                </div>
              </div>
            </div>

            {/* Optional Fields Section */}
            <div className="space-y-3">
              <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
                <FileText className="h-4 w-4 text-slate-500" />
                Optional Information
              </h3>
              
              <div className="space-y-1.5">
                <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                  TLS Suffix
                </Label>
                <Input 
                  value={tlsSuffix} 
                  onChange={(e) => setTlsSuffix(e.target.value)}
                  placeholder="Additional code suffix (if any)"
                  className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                />
              </div>

              <div className="space-y-1.5">
                <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                  Note
                </Label>
                <Input 
                  value={note} 
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Additional notes or comments"
                  className="border-2 border-slate-300 dark:border-slate-600 focus:border-blue-500"
                />
              </div>
            </div>
          </div>

          <DialogFooter className="pt-4 border-t border-slate-200 dark:border-slate-700 gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setIsAddDialogOpen(false)
                setTimeout(resetAddForm, 300)
              }}
              disabled={loading}
              className="min-w-[120px]"
            >
              Cancel
            </Button>
            <Button
              type="button"
              onClick={handleCreateTariff}
              disabled={loading}
              className="min-w-[120px] bg-gradient-to-r from-green-600 to-green-700 hover:from-green-700 hover:to-green-800 text-white font-semibold"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Creating...
                </>
              ) : (
                <>
                  <Plus className="h-4 w-4 mr-2" />
                  Create Tariff
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog - Reusing from EditTariffTab */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit Tariff #{selectedTariff?.tariffId}</DialogTitle>
            <DialogDescription>Update tariff information</DialogDescription>
          </DialogHeader>

          {selectedTariff && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Ad Valorem Rate (%)</Label>
                  <Input
                    type="number"
                    value={editFormData.adValoremRate ?? ''}
                    onChange={(e) => setEditFormData({ ...editFormData, adValoremRate: e.target.value ? parseFloat(e.target.value) : undefined })}
                  />
                </div>
                <div>
                  <Label>Specific Rate</Label>
                  <Input
                    type="number"
                    value={editFormData.specificRate ?? ''}
                    onChange={(e) => setEditFormData({ ...editFormData, specificRate: e.target.value ? parseFloat(e.target.value) : undefined })}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Compound Rate 1</Label>
                  <Input
                    type="number"
                    value={editFormData.compoundRate1 ?? ''}
                    onChange={(e) => setEditFormData({ ...editFormData, compoundRate1: e.target.value ? parseFloat(e.target.value) : undefined })}
                  />
                </div>
                <div>
                  <Label>Compound Rate 2</Label>
                  <Input
                    type="number"
                    value={editFormData.compoundRate2 ?? ''}
                    onChange={(e) => setEditFormData({ ...editFormData, compoundRate2: e.target.value ? parseFloat(e.target.value) : undefined })}
                  />
                </div>
              </div>

              <div>
                <Label>Specific Rate Unit</Label>
                <Input
                  value={editFormData.specificRateUnit}
                  onChange={(e) => setEditFormData({ ...editFormData, specificRateUnit: e.target.value })}
                />
              </div>

              <div>
                <Label>TLS Suffix</Label>
                <Input
                  value={editFormData.tlsSuffix}
                  onChange={(e) => setEditFormData({ ...editFormData, tlsSuffix: e.target.value })}
                />
              </div>

              <div>
                <Label>Note</Label>
                <Input
                  value={editFormData.note}
                  onChange={(e) => setEditFormData({ ...editFormData, note: e.target.value })}
                />
              </div>
            </div>
          )}

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>Cancel</Button>
            <Button onClick={handleSaveTariff} disabled={loading}>
              {loading ? "Saving..." : "Save Changes"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteConfirmation.show} onOpenChange={(open: boolean) => !open && cancelDelete()}>
        <DialogContent className="bg-white dark:bg-slate-900 border-2 border-red-300 dark:border-red-700">
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold text-red-600 dark:text-red-400 flex items-center gap-2">
              <AlertCircle className="h-6 w-6" />
              Confirm Deletion
            </DialogTitle>
            <DialogDescription>
              This action cannot be undone. This will permanently delete the tariff from the database.
            </DialogDescription>
          </DialogHeader>

          <div className="py-4">
            <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              <pre className="text-sm whitespace-pre-wrap text-red-900 dark:text-red-100">
                {deleteConfirmation.tariffDetails}
              </pre>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={cancelDelete} disabled={loading}>Cancel</Button>
            <Button
              onClick={() => deleteConfirmation.tariffId && handleDeleteTariff(deleteConfirmation.tariffId)}
              disabled={loading}
              className="bg-red-600 hover:bg-red-700"
            >
              {loading ? "Deleting..." : "Delete Tariff"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Notification Popup */}
      {notification.show && (
        <div className="fixed inset-0 bg-black/20 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className={`w-full max-w-md mx-auto rounded-lg shadow-xl border transform transition-all duration-300 ${
            notification.type === 'success' 
              ? 'bg-green-50 dark:bg-green-900/30 border-green-300 dark:border-green-700' 
              : notification.type === 'error'
              ? 'bg-red-50 dark:bg-red-900/30 border-red-300 dark:border-red-700'
              : 'bg-yellow-50 dark:bg-yellow-900/30 border-yellow-300 dark:border-yellow-700'
          }`}>
            <div className="p-4">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-start gap-3">
                  {notification.type === 'success' ? (
                    <CheckCircle className="h-6 w-6 text-green-600 dark:text-green-400 flex-shrink-0 mt-0.5" />
                  ) : notification.type === 'error' ? (
                    <XCircle className="h-6 w-6 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" />
                  ) : (
                    <AlertCircle className="h-6 w-6 text-yellow-600 dark:text-yellow-400 flex-shrink-0 mt-0.5" />
                  )}
                  <h3 className={`font-bold text-lg ${
                    notification.type === 'success' 
                      ? 'text-green-900 dark:text-green-100'
                      : notification.type === 'error'
                      ? 'text-red-900 dark:text-red-100'
                      : 'text-yellow-900 dark:text-yellow-100'
                  }`}>
                    {notification.title}
                  </h3>
                </div>
                <button
                  onClick={hideNotification}
                  className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                >
                  ✕
                </button>
              </div>
              
              <p className={`text-sm mb-3 ${
                notification.type === 'success' 
                  ? 'text-green-800 dark:text-green-200'
                  : notification.type === 'error'
                  ? 'text-red-800 dark:text-red-200'
                  : 'text-yellow-800 dark:text-yellow-200'
              }`}>
                {notification.message}
              </p>
              
              {notification.details && (
                <div className="mt-2 p-3 bg-white/50 dark:bg-black/20 rounded border border-slate-200 dark:border-slate-600">
                  <pre className="text-xs whitespace-pre-wrap text-slate-700 dark:text-slate-300">
                    {notification.details}
                  </pre>
                </div>
              )}
              
              <div className="flex justify-end">
                <Button
                  onClick={hideNotification}
                  className={`${
                    notification.type === 'success'
                      ? 'bg-green-600 hover:bg-green-700'
                      : notification.type === 'error'
                      ? 'bg-red-600 hover:bg-red-700'
                      : 'bg-yellow-600 hover:bg-yellow-700'
                  }`}
                >
                  OK
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Status Messages */}
      {(error || success) && (
        <div className={`flex items-start gap-2 p-3 rounded-lg text-sm font-medium ${
          success
            ? 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-800'
            : 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 border border-red-200 dark:border-red-800'
        }`}>
          {success ? (
            <CheckCircle className="h-4 w-4 mt-0.5 text-green-600 dark:text-green-400 flex-shrink-0" />
          ) : (
            <XCircle className="h-4 w-4 mt-0.5 text-red-600 dark:text-red-400 flex-shrink-0" />
          )}
          <span className="flex-1 break-words">{error || success}</span>
        </div>
      )}
    </div>
  )
}
