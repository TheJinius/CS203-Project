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

  // Add tariff dialog
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false)

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

      // Validate duty rates - at least one rate must be provided
      const hasAdValorem = adValoremRate && adValoremRate.trim() !== "" && parseFloat(adValoremRate) >= 0
      const hasSpecific = specificRate && specificRate.trim() !== "" && parseFloat(specificRate) >= 0
      const hasCompound1 = compoundRate1 && compoundRate1.trim() !== "" && parseFloat(compoundRate1) >= 0
      const hasCompound2 = compoundRate2 && compoundRate2.trim() !== "" && parseFloat(compoundRate2) >= 0
      
      if (!hasAdValorem && !hasSpecific && !hasCompound1 && !hasCompound2) {
        throw new Error("At least one duty rate must be specified (Ad Valorem, Specific, or Combined rates)")
      }
      
      // If specific or combined rates are used, unit is required
      if ((hasSpecific || hasCompound2) && (!specificRateUnit || specificRateUnit.trim() === "")) {
        throw new Error("Specific rate unit is required when using specific or combined duties")
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

      // Add duty rates - backend determines duty nature from which rates are provided
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
        let errorMessage = `Failed to create tariff (${response.status})`
        try {
          const errorText = await response.text()
          console.error("❌ Error response:", errorText)
          
          // Try to parse as JSON first (backend might send structured error)
          try {
            const errorJson = JSON.parse(errorText)
            errorMessage = errorJson.message || errorJson.error || errorText || errorMessage
          } catch {
            // Not JSON, use raw text
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

      // Close dialog and reset form after success
      setIsAddDialogOpen(false)
      setTimeout(() => {
        resetForm()
      }, 500)

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
    <>
    <Card className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700">
      <CardHeader className="pb-4 border-b border-slate-200 dark:border-slate-700">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-lg flex items-center gap-2 text-slate-900 dark:text-slate-100">
              <Search className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              Manage Tariffs
            </CardTitle>
            <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
              View, add, and delete tariff entries
            </p>
          </div>
          <Button
            onClick={() => setIsAddDialogOpen(true)}
            className="bg-green-600 hover:bg-green-700 text-white"
          >
            <Plus className="h-4 w-4 mr-2" />
            Add New Tariff
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-4 pt-4">
        
        {/* Load All Tariffs Button */}
        <Button
          onClick={handleSearchAllTariffs}
          disabled={loading}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white"
        >
          {loading ? (
            <>
              <span className="animate-spin mr-2">⏳</span>
              Loading...
            </>
          ) : (
            <>
              <Search className="h-4 w-4 mr-2" />
              Load All Tariffs
            </>
          )}
        </Button>

        {/* Tariff List */}
        {searchResults.length > 0 && (
          <div className="space-y-3">
            <div className="p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
              <p className="text-sm text-blue-900 dark:text-blue-100">
                <strong>Showing {searchResults.length} tariff(s)</strong>
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
                            {tariff.dutyCategory === 'A' ? 'Ad Valorem' : 
                             tariff.dutyCategory === 'S' ? 'Specific' :
                             tariff.dutyCategory === 'C' ? 'Combined' : 'Other'}
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
            <span className="flex-1 break-words">{success || error}</span>
          </div>
        )}
        
      </CardContent>

    </Card>

      {/* Add Tariff Dialog */}
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
                      {dt.dutyType}-{dt.dutyCode}: {dt.description}
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
            <p className="text-xs text-blue-600 dark:text-blue-400 flex items-center gap-1">
              <AlertCircle className="h-3 w-3" />
              You can enter any HS code. New codes will be auto-created.
            </p>
            <div className="relative w-full">
              <Input
                value={productSearchQuery}
                onChange={(e) => handleProductSearchChange(e.target.value)}
                placeholder="Enter or search HS Code (e.g., 27079940, 999999)"
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
              Duty Rates <span className="text-red-500 text-xs">(At least one rate required)</span>
            </h3>
            {currentDutyType && (
              <span className="text-xs font-semibold px-2 py-1 rounded-full bg-amber-100 dark:bg-amber-900 text-amber-800 dark:text-amber-200">
                Detected: {currentDutyType === 'AD_VALOREM' ? 'Ad Valorem' : 
                          currentDutyType === 'SPECIFIC' ? 'Specific' :
                          currentDutyType === 'COMBINED' ? 'Combined' : 'Other'}
              </span>
            )}
          </div>
          
          <div className="p-4 bg-amber-50 dark:bg-amber-900/10 rounded-lg border border-amber-200 dark:border-amber-800 space-y-4">
            <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded">
              <p className="text-xs text-blue-900 dark:text-blue-100">
                <strong>How it works:</strong> Enter any combination of rates below. The system will automatically determine the duty type:
              </p>
              <ul className="text-xs text-blue-800 dark:text-blue-200 mt-1 ml-4 list-disc space-y-0.5">
                <li><strong>Ad Valorem only:</strong> Enter percentage rate only</li>
                <li><strong>Specific only:</strong> Enter amount and unit only</li>
                <li><strong>Combined:</strong> Enter both percentage and amount+unit</li>
              </ul>
            </div>

            {/* AD VALOREM RATE */}
            <div className="space-y-2">
              <Label className="text-sm font-semibold text-amber-900 dark:text-amber-100">
                Ad Valorem Rate (%)
              </Label>
              <Input
                type="number"
                value={adValoremRate}
                onChange={(e) => setAdValoremRate(e.target.value)}
                placeholder="e.g., 20 (for 20%)"
                className="border-2 border-amber-300 dark:border-amber-700 w-full"
                min="0"
                max="100"
                step="0.01"
              />
              <p className="text-xs text-amber-700 dark:text-amber-300">
                Percentage of goods value (leave empty if not applicable)
              </p>
            </div>

            {/* SPECIFIC RATE */}
            <div className="space-y-2">
              <Label className="text-sm font-semibold text-amber-900 dark:text-amber-100">
                Specific Rate (Amount + Unit)
              </Label>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <Input
                    type="number"
                    value={specificRate}
                    onChange={(e) => setSpecificRate(e.target.value)}
                    placeholder="e.g., 0.09"
                    className="border-2 border-amber-300 dark:border-amber-700 w-full"
                    min="0"
                    step="0.01"
                  />
                  <p className="text-xs text-amber-700 dark:text-amber-300 mt-1">
                    Amount (e.g., $0.09)
                  </p>
                </div>
                <div>
                  <Input
                    type="text"
                    value={specificRateUnit}
                    onChange={(e) => setSpecificRateUnit(e.target.value)}
                    placeholder="e.g., kg"
                    className="border-2 border-amber-300 dark:border-amber-700 w-full"
                  />
                  <p className="text-xs text-amber-700 dark:text-amber-300 mt-1">
                    Unit (e.g., kg, L, unit)
                  </p>
                </div>
              </div>
              <p className="text-xs text-amber-700 dark:text-amber-300">
                Fixed amount per unit (leave empty if not applicable)
              </p>
            </div>

            {/* COMBINED RATES (Alternative entry method) */}
            <div className="space-y-2 pt-2 border-t border-amber-300 dark:border-amber-700">
              <Label className="text-sm font-semibold text-amber-900 dark:text-amber-100 flex items-center gap-2">
                <AlertCircle className="h-4 w-4" />
                Alternative: Combined Rates (Compound1 + Compound2)
              </Label>
              <p className="text-xs text-amber-700 dark:text-amber-300 mb-2">
                Use these fields if you need to specify combined duty differently (will override Ad Valorem and Specific fields above)
              </p>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <Input
                    type="number"
                    value={compoundRate1}
                    onChange={(e) => setCompoundRate1(e.target.value)}
                    placeholder="Compound Rate 1 (%)"
                    className="border-2 border-amber-300 dark:border-amber-700 w-full"
                    min="0"
                    step="0.01"
                  />
                  <p className="text-xs text-amber-700 dark:text-amber-300 mt-1">
                    Percentage component
                  </p>
                </div>
                <div>
                  <Input
                    type="number"
                    value={compoundRate2}
                    onChange={(e) => setCompoundRate2(e.target.value)}
                    placeholder="Compound Rate 2 (amount)"
                    className="border-2 border-amber-300 dark:border-amber-700 w-full"
                    min="0"
                    step="0.01"
                  />
                  <p className="text-xs text-amber-700 dark:text-amber-300 mt-1">
                    Amount component
                  </p>
                </div>
              </div>
            </div>

            {/* Combined Mode Selector */}
            {currentDutyType === 'COMBINED' && (
              <div className="p-3 bg-purple-50 dark:bg-purple-900/20 border border-purple-200 dark:border-purple-800 rounded">
                <Label className="text-sm font-semibold text-purple-900 dark:text-purple-100 mb-2 block">
                  Combined Duty Calculation Mode
                </Label>
                <Select value={combinedDutyMode} onValueChange={(value: 'M' | 'C') => setCombinedDutyMode(value)}>
                  <SelectTrigger className="border-2 border-purple-300 dark:border-purple-700">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="M">Mixed (Maximum) - Take the higher value</SelectItem>
                    <SelectItem value="C">Compound (Sum) - Add both values together</SelectItem>
                  </SelectContent>
                </Select>
                <p className="text-xs text-purple-700 dark:text-purple-300 mt-1">
                  {combinedDutyMode === 'M' 
                    ? '✓ Will use whichever is higher: percentage OR specific amount' 
                    : '✓ Will add both: percentage + specific amount'}
                </p>
              </div>
            )}
          </div>
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
          </div>

          <DialogFooter className="pt-4 border-t border-slate-200 dark:border-slate-700 gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setIsAddDialogOpen(false)
                resetForm()
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
                  <span className="animate-spin mr-2">⏳</span>
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
    </>
  )
}