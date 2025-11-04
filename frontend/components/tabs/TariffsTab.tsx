"use client"

import { useState, useCallback, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Plus, CheckCircle, XCircle, AlertCircle, Trash2, Search } from "lucide-react"
import { getSession, signOut } from "next-auth/react"
import { searchProducts as apiSearchProducts } from "@/lib/api"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../ui/dialog"
import AdValoremDutyForm from "./duty-forms/AdValoremDutyForm"
import SpecificDutyForm from "./duty-forms/SpecificDutyForm"
import CombinedDutyForm from "./duty-forms/CombinedDutyForm"
import OtherDutyForm from "./duty-forms/OtherDutyForm"

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
}

interface DeleteConfirmation {
  show: boolean
  tariffId: number | null
  tariffDetails: string
}

export default function TariffsTab() {
  // Form state
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

  // Search/View tariffs
  const [viewMode, setViewMode] = useState<'create' | 'manage'>('create')
  const [searchResults, setSearchResults] = useState<TariffData[]>([])
  const [searchQuery, setSearchQuery] = useState<string>("")

  const predefinedProducts = [
    { code: "27079940", description: "Carbazole, Energy" },
    { code: "1012100", description: "Pure Bred Breeding Horses" },
    { code: "29092000", description: "Cyclanic, Pharmaceutical" },
    { code: "74130000", description: "Copper Wire" }
  ]

  // Common duty types (you can expand this list)
  const dutyTypes: DutyType[] = [
    { dutyType: "AV", dutyCode: "01", description: "Ad Valorem" },
    { dutyType: "SP", dutyCode: "01", description: "Specific" },
    { dutyType: "CO", dutyCode: "01", description: "Compound/Combined" },
    { dutyType: "OT", dutyCode: "01", description: "Other" },
  ]

  const showNotification = (type: 'success' | 'error' | 'warning', title: string, message: string, details?: string) => {
    setNotification({ show: true, type, title, message, details })
  }

  const hideNotification = () => {
    setNotification(prev => ({ ...prev, show: false }))
  }

  // Update current duty type when duty type selection changes
  useEffect(() => {
    if (dutyType) {
      // Map duty type codes to our internal enum
      const upperDutyType = dutyType.toUpperCase()
      if (upperDutyType === 'AV') {
        setCurrentDutyType('AD_VALOREM')
      } else if (upperDutyType === 'SP') {
        setCurrentDutyType('SPECIFIC')
      } else if (upperDutyType === 'CO') {
        setCurrentDutyType('COMBINED')
      } else if (upperDutyType === 'OT') {
        setCurrentDutyType('OTHER')
      } else {
        setCurrentDutyType(null)
      }
    } else {
      setCurrentDutyType(null)
    }
  }, [dutyType])

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
    setProductCode(product.code)
    setProductSearchQuery(`${product.code} - ${product.description}`)
    setProductSearchResults([])
  }

  // Auto-set product code if user types a numeric code directly
  const handleProductSearchChange = (value: string) => {
    setProductSearchQuery(value)
    
    // If user types only numbers (likely an HS code), auto-set it as product code
    const trimmedValue = value.trim()
    if (/^\d+$/.test(trimmedValue) && trimmedValue.length >= 4) {
      setProductCode(trimmedValue)
    } else if (trimmedValue === "") {
      setProductCode("")
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

  const resetForm = () => {
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

  const handleCreateTariff = async () => {
    setLoading(true)
    setError("")
    setSuccess("")

    try {
      // Validation - check each field individually for better error messages
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

      // Validate duty rates based on duty type
      if (currentDutyType === 'AD_VALOREM') {
        if (!adValoremRate || adValoremRate.trim() === "") {
          throw new Error("Ad Valorem rate is required for Ad Valorem duty type")
        }
      } else if (currentDutyType === 'SPECIFIC') {
        if (!specificRate || specificRate.trim() === "") {
          throw new Error("Specific rate is required for Specific duty type")
        }
        if (!specificRateUnit || specificRateUnit.trim() === "") {
          throw new Error("Specific rate unit is required for Specific duty type")
        }
      } else if (currentDutyType === 'COMBINED') {
        if (!compoundRate1 || compoundRate1.trim() === "" || !compoundRate2 || compoundRate2.trim() === "") {
          throw new Error("Both compound rates are required for Combined duty type")
        }
        if (!specificRateUnit || specificRateUnit.trim() === "") {
          throw new Error("Specific rate unit is required for Combined duty type")
        }
      } else if (currentDutyType === 'OTHER') {
        // OTHER type doesn't require specific rates, can proceed
      } else {
        throw new Error("Please select a duty type and fill in the required duty rates")
      }

      const headers = await getAuthHeaders()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

      // Build request body
      const requestBody: Record<string, any> = {
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

      // Add duty rates based on duty type
      if (currentDutyType === 'AD_VALOREM' && adValoremRate.trim() !== "") {
        const value = parseFloat(adValoremRate)
        if (!isNaN(value) && value >= 0) {
          requestBody.adValoremRate = value
        }
      } else if (currentDutyType === 'SPECIFIC' && specificRate.trim() !== "") {
        const value = parseFloat(specificRate)
        if (!isNaN(value) && value >= 0) {
          requestBody.specificRate = value
        }
      } else if (currentDutyType === 'COMBINED') {
        if (compoundRate1.trim() !== "") {
          const value = parseFloat(compoundRate1)
          if (!isNaN(value) && value >= 0) {
            requestBody.compoundRate1 = value
          }
        }
        if (compoundRate2.trim() !== "") {
          const value = parseFloat(compoundRate2)
          if (!isNaN(value) && value >= 0) {
            requestBody.compoundRate2 = value
          }
        }
      }

      console.log("📤 Creating tariff with data:", JSON.stringify(requestBody, null, 2))

      const response = await fetch(`${apiUrl}/api/admin/tariffs`, {
        method: 'POST',
        headers,
        body: JSON.stringify(requestBody),
        mode: 'cors',
        credentials: 'include',
      })

      console.log("📡 Response status:", response.status)

      if (!response.ok) {
        const errorText = await response.text()
        console.error("❌ Error response:", errorText)
        throw new Error(`Failed to create tariff (${response.status}): ${errorText}`)
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

      // Reset form after success
      setTimeout(() => {
        resetForm()
      }, 2000)

    } catch (e) {
      const err = e as Error
      console.error("❌ Create failed:", err)
      setError(err.message)
      
      showNotification(
        'error',
        'Creation Failed ❌',
        'Failed to create tariff',
        err.message
      )
    } finally {
      setLoading(false)
    }
  }

  const handleSearchAllTariffs = async () => {
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
      
      setSearchResults(tariffs)
      setSuccess(`Found ${tariffs.length} tariff(s)`)
      setViewMode('manage')
    } catch (e) {
      const err = e as Error
      console.error("❌ Search failed:", err)
      setError(`Failed to load tariffs: ${err.message}`)
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteTariff = async (tariffId: number) => {
    setLoading(true)
    
    try {
      const headers = await getAuthHeaders()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
      
      console.log("🗑️ Deleting tariff:", tariffId)
      
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
      
      // Close confirmation dialog
      setDeleteConfirmation({ show: false, tariffId: null, tariffDetails: '' })
      
      // Refresh the list
      await handleSearchAllTariffs()
      
    } catch (e) {
      const err = e as Error
      console.error("❌ Delete failed:", err)
      setError(`Delete failed: ${err.message}`)
      
      showNotification(
        'error',
        'Delete Failed ❌',
        `Failed to delete tariff ${tariffId}`,
        err.message
      )
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
    <Card className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700">
      <CardHeader className="pb-4 border-b border-slate-200 dark:border-slate-700">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-lg flex items-center gap-2 text-slate-900 dark:text-slate-100">
              {viewMode === 'create' ? (
                <>
                  <Plus className="h-5 w-5 text-green-600 dark:text-green-400" />
                  Add New Tariff
                </>
              ) : (
                <>
                  <Search className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                  Manage Tariffs
                </>
              )}
            </CardTitle>
            <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
              {viewMode === 'create' 
                ? 'Create a new tariff entry in the database'
                : 'View and delete existing tariffs'}
            </p>
          </div>
          <Button
            variant="outline"
            onClick={() => {
              if (viewMode === 'create') {
                handleSearchAllTariffs()
              } else {
                setViewMode('create')
                setSearchResults([])
              }
            }}
            className="border-2"
          >
            {viewMode === 'create' ? (
              <>
                <Search className="h-4 w-4 mr-2" />
                Manage Tariffs
              </>
            ) : (
              <>
                <Plus className="h-4 w-4 mr-2" />
                Add New
              </>
            )}
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4 pt-4">
        
        {viewMode === 'create' ? (
          // CREATE MODE - Existing form
          <>
        {/* Required Fields Section */}
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
            <span className="h-1 w-1 rounded-full bg-red-500"></span>
            Required Information
          </h3>
          
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
            {/* Year */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Tariff Year <span className="text-red-500">*</span>
              </Label>
              <Input
                type="number"
                value={tariffYear}
                onChange={(e) => setTariffYear(e.target.value)}
                className="border-2 border-slate-300 dark:border-slate-600 w-full"
                min="2000"
                max="2100"
              />
            </div>

            {/* Reporter Country */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Reporter Country <span className="text-red-500">*</span>
              </Label>
              <Select value={reporterCode} onValueChange={setReporterCode}>
                <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600 w-full">
                  <SelectValue placeholder="Select reporter" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="702">702 - Singapore</SelectItem>
                  <SelectItem value="840">840 - United States</SelectItem>
                  <SelectItem value="156">156 - China</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Partner Country */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Partner Country <span className="text-red-500">*</span>
              </Label>
              <Select value={partnerCode} onValueChange={setPartnerCode}>
                <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600 w-full">
                  <SelectValue placeholder="Select partner" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="702">702 - Singapore</SelectItem>
                  <SelectItem value="840">840 - United States</SelectItem>
                  <SelectItem value="156">156 - China</SelectItem>
                  <SelectItem value="000">000 - World (Any Country)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Duty Type */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Duty Type <span className="text-red-500">*</span>
              </Label>
              <Select 
                value={dutyType ? `${dutyType}-${dutyCode}` : ""} 
                onValueChange={(value) => {
                  const [type, code] = value.split('-')
                  setDutyType(type)
                  setDutyCode(code)
                }}
              >
                <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600 w-full">
                  <SelectValue placeholder="Select duty type" />
                </SelectTrigger>
                <SelectContent>
                  {dutyTypes.map((dt) => (
                    <SelectItem key={`${dt.dutyType}-${dt.dutyCode}`} value={`${dt.dutyType}-${dt.dutyCode}`}>
                      {dt.dutyType}-{dt.dutyCode} - {dt.description}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Product Search */}
          <div className="space-y-1.5">
            <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              Product (HS Code) <span className="text-red-500">*</span>
            </Label>
            <div className="relative w-full">
              <Input
                value={productSearchQuery}
                onChange={(e) => handleProductSearchChange(e.target.value)}
                placeholder="Search by HS Code or description (e.g., 999999)"
                className="border-2 border-slate-300 dark:border-slate-600 w-full"
              />
              {productSearchResults.length > 0 && productSearchQuery && (
                <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-md shadow-lg max-h-60 overflow-y-auto">
                  {productSearchResults.map((product, index) => (
                    <button
                      key={`${product.code}-${index}`}
                      type="button"
                      onClick={() => handleProductSelect(product)}
                      className="w-full text-left px-3 py-2 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-900 dark:text-slate-100 border-b border-slate-200 dark:border-slate-600 last:border-b-0"
                    >
                      <div className="font-medium text-blue-600 dark:text-blue-400">{product.code}</div>
                      <div className="text-sm text-slate-600 dark:text-slate-400 mt-1 line-clamp-2">
                        {product.description}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
            {productCode && (
              <p className="text-xs text-green-600 dark:text-green-400">
                ✓ Selected: {productCode}
              </p>
            )}
          </div>
        </div>

        {/* Optional Fields */}
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
            <span className="h-1 w-1 rounded-full bg-blue-500"></span>
            Optional Details
          </h3>
          
          <div className="space-y-1.5">
            <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              TLS Suffix
            </Label>
            <Input
              value={tlsSuffix}
              onChange={(e) => setTlsSuffix(e.target.value)}
              placeholder="e.g., A, B, 01"
              className="border-2 border-blue-300 dark:border-blue-700 w-full max-w-md"
            />
            <p className="text-xs text-blue-600 dark:text-blue-400">
              Additional classification suffix (optional)
            </p>
          </div>
        </div>

        {/* Duty Rates Section */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
              <span className="h-1 w-1 rounded-full bg-amber-500"></span>
              Duty Rates <span className="text-red-500 text-xs">(Required - select duty type first)</span>
            </h3>
          </div>
          
          {!currentDutyType ? (
            <div className="p-6 bg-slate-100 dark:bg-slate-700 border-2 border-dashed border-slate-300 dark:border-slate-600 rounded-lg text-center">
              <AlertCircle className="h-8 w-8 text-slate-400 dark:text-slate-500 mx-auto mb-2" />
              <p className="text-sm text-slate-600 dark:text-slate-400 font-medium">
                Please select a Duty Type above to configure duty rates
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-500 mt-1">
                The form will adapt based on your selection (Ad Valorem, Specific, Combined, or Other)
              </p>
            </div>
          ) : (
            <div className="p-4 bg-amber-50 dark:bg-amber-900/10 rounded-lg border border-amber-200 dark:border-amber-800">
              {/* AD VALOREM DUTY FORM */}
              {currentDutyType === 'AD_VALOREM' && (
                <AdValoremDutyForm
                  adValoremRate={adValoremRate ? parseFloat(adValoremRate) : undefined}
                  onChange={(value) => {
                    setAdValoremRate(value !== undefined ? value.toString() : "")
                    // Clear other rates
                    setSpecificRate("")
                    setCompoundRate1("")
                    setCompoundRate2("")
                  }}
                />
              )}

              {/* SPECIFIC DUTY FORM */}
              {currentDutyType === 'SPECIFIC' && (
                <SpecificDutyForm
                  specificRate={specificRate ? parseFloat(specificRate) : undefined}
                  specificRateUnit={specificRateUnit}
                  onRateChange={(value) => {
                    setSpecificRate(value !== undefined ? value.toString() : "")
                    // Clear other rates
                    setAdValoremRate("")
                    setCompoundRate1("")
                    setCompoundRate2("")
                  }}
                  onUnitChange={(value) => {
                    setSpecificRateUnit(value)
                  }}
                />
              )}

              {/* COMBINED DUTY FORM */}
              {currentDutyType === 'COMBINED' && (
                <CombinedDutyForm
                  compoundRate1={compoundRate1 ? parseFloat(compoundRate1) : undefined}
                  compoundRate2={compoundRate2 ? parseFloat(compoundRate2) : undefined}
                  specificRateUnit={specificRateUnit}
                  combinedMode={combinedDutyMode}
                  onRate1Change={(value) => {
                    setCompoundRate1(value !== undefined ? value.toString() : "")
                  }}
                  onRate2Change={(value) => {
                    setCompoundRate2(value !== undefined ? value.toString() : "")
                  }}
                  onUnitChange={(value) => {
                    setSpecificRateUnit(value)
                  }}
                  onModeChange={(mode) => setCombinedDutyMode(mode)}
                />
              )}

              {/* OTHER DUTY FORM */}
              {currentDutyType === 'OTHER' && (
                <OtherDutyForm />
              )}
            </div>
          )}
        </div>

        {/* Notes Section */}
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
            <span className="h-1 w-1 rounded-full bg-purple-500"></span>
            Additional Notes
          </h3>
          <div className="space-y-1.5">
            <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              Notes (Max 1000 characters)
            </Label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={3}
              className="w-full p-3 border-2 border-purple-300 dark:border-purple-700 rounded-md bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 resize-none break-words whitespace-pre-wrap"
              placeholder="Add any additional notes or comments about this tariff..."
              maxLength={1000}
              style={{ wordWrap: 'break-word', overflowWrap: 'break-word' }}
            />
            <div className="flex justify-end text-xs text-slate-500 dark:text-slate-400">
              <span>{note.length} / 1000 characters</span>
            </div>
          </div>
        </div>

        {/* Action Button */}
        <Button
          onClick={handleCreateTariff}
          disabled={loading}
          className="w-full bg-gradient-to-r from-green-600 to-green-700 hover:from-green-700 hover:to-green-800 text-white font-semibold shadow-lg h-12"
        >
          {loading ? (
            <>
              <span className="animate-spin mr-2">⏳</span>
              Creating Tariff...
            </>
          ) : (
            <>
              <Plus className="h-5 w-5 mr-2" />
              Create New Tariff
            </>
          )}
        </Button>

        {/* Status Messages for Create Mode */}
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
            <span className="flex-1 break-words">{success || error}</span>
          </div>
        )}
          </>
        ) : (
          // MANAGE MODE - View and Delete Tariffs
          <>
            <div className="space-y-3">
              <div className="p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                <p className="text-sm text-blue-900 dark:text-blue-100">
                  <strong>Showing {searchResults.length} tariff(s)</strong>
                </p>
                <p className="text-xs text-blue-700 dark:text-blue-300 mt-1">
                  Click the delete button to remove a tariff. You will be asked to confirm before deletion.
                </p>
              </div>

              <div className="max-h-[600px] overflow-y-auto space-y-3">
                {searchResults.map((tariff) => (
                  <div
                    key={tariff.tariffId}
                    className="p-4 border-2 border-slate-200 dark:border-slate-600 rounded-lg bg-slate-50 dark:bg-slate-700 hover:border-blue-400 dark:hover:border-blue-500 transition-colors"
                  >
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <span className="font-bold text-blue-600 dark:text-blue-400">
                            ID: {tariff.tariffId}
                          </span>
                          <span className="text-xs bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 px-2 py-1 rounded">
                            {tariff.tariffYear}
                          </span>
                          {tariff.dutyCategory && (
                            <span className="text-xs bg-amber-100 dark:bg-amber-900 text-amber-800 dark:text-amber-200 px-2 py-1 rounded">
                              {tariff.dutyCategory.replace(/_/g, ' ')}
                            </span>
                          )}
                        </div>
                        <div className="text-sm space-y-1 text-slate-700 dark:text-slate-300">
                          <div><strong>Route:</strong> {tariff.partnerName} ({tariff.partnerCode}) → {tariff.reporterName} ({tariff.reporterCode})</div>
                          <div><strong>Product:</strong> {tariff.tlCode} - {tariff.productDescription}</div>
                          <div><strong>Duty Type:</strong> {tariff.dutyType}-{tariff.dutyCode} ({tariff.dutyTypeDescription})</div>
                          {tariff.adValoremRate !== null && tariff.adValoremRate !== undefined && (
                            <div><strong>Ad Valorem:</strong> {tariff.adValoremRate}%</div>
                          )}
                          {tariff.specificRate !== null && tariff.specificRate !== undefined && (
                            <div><strong>Specific:</strong> {tariff.specificRate} {tariff.specificRateUnit}</div>
                          )}
                          {tariff.compoundRate1 !== null && tariff.compoundRate1 !== undefined && (
                            <div><strong>Compound:</strong> {tariff.compoundRate1}% + {tariff.compoundRate2} {tariff.specificRateUnit}</div>
                          )}
                        </div>
                      </div>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => confirmDelete(tariff)}
                        disabled={loading}
                        className="text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20 border-red-300 dark:border-red-700"
                      >
                        <Trash2 className="h-4 w-4 mr-1" />
                        Delete
                      </Button>
                    </div>
                  </div>
                ))}
                
                {searchResults.length === 0 && !loading && (
                  <div className="text-center p-8 text-slate-500 dark:text-slate-400">
                    <Search className="h-12 w-12 mx-auto mb-3 opacity-50" />
                    <p>No tariffs found. Click "Manage Tariffs" to load all tariffs.</p>
                  </div>
                )}
              </div>
            </div>

            {/* Status Messages for Manage Mode */}
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
                <span className="flex-1 break-words">{success || error}</span>
              </div>
            )}
          </>
        )}
        
      </CardContent>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteConfirmation.show} onOpenChange={(open: boolean) => !open && cancelDelete()}>
        <DialogContent className="bg-white dark:bg-slate-900 border-2 border-red-300 dark:border-red-700">
          <DialogHeader>
            <DialogTitle className="text-2xl font-bold text-red-600 dark:text-red-400 flex items-center gap-2">
              <AlertCircle className="h-6 w-6" />
              Confirm Deletion
            </DialogTitle>
            <DialogDescription className="text-slate-600 dark:text-slate-400 mt-2">
              This action cannot be undone. This will permanently delete the tariff from the database.
            </DialogDescription>
          </DialogHeader>

          <div className="py-4">
            <div className="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              <p className="text-sm font-semibold text-red-900 dark:text-red-100 mb-2">
                You are about to delete:
              </p>
              <pre className="text-xs text-red-800 dark:text-red-200 whitespace-pre-line font-mono">
                {deleteConfirmation.tariffDetails}
              </pre>
            </div>

            <div className="mt-4 p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
              <p className="text-xs font-semibold text-yellow-900 dark:text-yellow-100 flex items-center gap-1">
                <AlertCircle className="h-3 w-3" />
                Warning
              </p>
              <p className="text-xs text-yellow-800 dark:text-yellow-200 mt-1">
                This will remove all associated duty information and cannot be recovered.
              </p>
            </div>
          </div>

          <DialogFooter className="gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={cancelDelete}
              disabled={loading}
              className="min-w-[120px]"
            >
              Cancel
            </Button>
            <Button
              type="button"
              onClick={() => deleteConfirmation.tariffId && handleDeleteTariff(deleteConfirmation.tariffId)}
              disabled={loading}
              className="min-w-[120px] bg-red-600 hover:bg-red-700 text-white"
            >
              {loading ? (
                <>
                  <span className="animate-spin mr-2">⏳</span>
                  Deleting...
                </>
              ) : (
                <>
                  <Trash2 className="h-4 w-4 mr-2" />
                  Yes, Delete
                </>
              )}
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
                <div className="flex items-center gap-2">
                  {notification.type === 'success' ? (
                    <CheckCircle className="h-6 w-6 text-green-600 dark:text-green-400 flex-shrink-0" />
                  ) : notification.type === 'error' ? (
                    <XCircle className="h-6 w-6 text-red-600 dark:text-red-400 flex-shrink-0" />
                  ) : (
                    <AlertCircle className="h-6 w-6 text-yellow-600 dark:text-yellow-400 flex-shrink-0" />
                  )}
                  <h3 className={`text-lg font-bold ${
                    notification.type === 'success' 
                      ? 'text-green-900 dark:text-green-100'
                      : notification.type === 'error'
                      ? 'text-red-900 dark:text-red-100'
                      : 'text-yellow-900 dark:text-yellow-100'
                  }`}>
                    {notification.title}
                  </h3>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={hideNotification}
                  className={`${
                    notification.type === 'success' 
                      ? 'text-green-600 hover:text-green-700 hover:bg-green-100'
                      : notification.type === 'error'
                      ? 'text-red-600 hover:text-red-700 hover:bg-red-100'
                      : 'text-yellow-600 hover:text-yellow-700 hover:bg-yellow-100'
                  }`}
                >
                  <XCircle className="h-4 w-4" />
                </Button>
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
                <div className={`p-3 rounded text-xs mb-3 ${
                  notification.type === 'success' 
                    ? 'bg-green-100 dark:bg-green-800/50 text-green-800 dark:text-green-200'
                    : notification.type === 'error'
                    ? 'bg-red-100 dark:bg-red-800/50 text-red-800 dark:text-red-200'
                    : 'bg-yellow-100 dark:bg-yellow-800/50 text-yellow-800 dark:text-yellow-200'
                }`}>
                  <div className="whitespace-pre-line font-mono">
                    {notification.details}
                  </div>
                </div>
              )}
              
              <div className="flex justify-end">
                <Button
                  size="sm"
                  onClick={hideNotification}
                  className={`${
                    notification.type === 'success' 
                      ? 'bg-green-600 hover:bg-green-700'
                      : notification.type === 'error'
                      ? 'bg-red-600 hover:bg-red-700'
                      : 'bg-yellow-600 hover:bg-yellow-700'
                  } text-white`}
                >
                  Got it
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </Card>
  )
}