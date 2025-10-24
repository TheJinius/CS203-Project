"use client"

import { useState, useEffect, useCallback } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Search, Edit, Plus, CheckCircle, XCircle, Trash2, X } from "lucide-react"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../ui/dialog"
import { getSession, signOut } from "next-auth/react"
import { searchTariffs, searchProducts as apiSearchProducts } from "@/lib/api"

interface Product {
  code: string
  tlCode?: string
  description?: string
  name?: string
  matchType?: string
}

interface TariffData {
  tariffId: number
  tariffYear: number  // Backend sends "year"
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

interface TariffRequest {
  tariffYear: number
  reporterCode: string
  partnerCode: string
  tlCode: string
  dutyType: string
  dutyCode: string
  tlsSuffix: string
  note: string
  adValoremRate?: number  // ✅ Optional means undefined when not present
  specificRate?: number    // ✅ Optional means undefined when not present
  specificRateUnit: string
  compoundRate1?: number   // ✅ Optional means undefined when not present
  compoundRate2?: number   // ✅ Optional means undefined when not present
}

interface NotificationPopup {
  show: boolean
  type: 'success' | 'error'
  title: string
  message: string
  details?: string
}

export default function EditTariffTab() {
  const [selectedProduct, setSelectedProduct] = useState<string>("")
  const [selectedSource, setSelectedSource] = useState<string>("")
  const [selectedDestination, setSelectedDestination] = useState<string>("")
  const [selectedYear, setSelectedYear] = useState<string>("2023")

  const [productSearchQuery, setProductSearchQuery] = useState<string>("")
  const [productSearchResults, setProductSearchResults] = useState<Array<{ code: string, description: string, matchType?: string }>>([])
  const [searchTimeout, setSearchTimeout] = useState<NodeJS.Timeout | null>(null)

  // ✅ ADD THESE MISSING STATE VARIABLES
  const [searchResults, setSearchResults] = useState<TariffData[]>([])
  const [searchParams, setSearchParams] = useState({
    reporterCountry: "",
    partnerCountry: "",
    productCode: "",
    year: 2023,
  })

  const [availableTariffs, setAvailableTariffs] = useState<TariffData[]>([])
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

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [step, setStep] = useState(1)

  const [notification, setNotification] = useState<NotificationPopup>({
    show: false,
    type: 'success',
    title: '',
    message: '',
    details: ''
  })

  const predefinedProducts = [
    { code: "27079940", description: "Carbazole, Energy" },
    { code: "1012100", description: "Pure Bred Breeding Horses" },
    { code: "29092000", description: "Cyclanic, Pharmaceutical" },
    { code: "74130000", description: "Copper Wire" }
  ]

  const showNotification = (type: 'success' | 'error', title: string, message: string, details?: string) => {
    setNotification({ show: true, type, title, message, details })
  }

  const hideNotification = () => {
    setNotification(prev => ({ ...prev, show: false }))
  }

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
    setSelectedProduct(product.code)
    setProductSearchQuery(`${product.code} - ${product.description}`)
    setProductSearchResults([])
  }

  const getAuthHeaders = useCallback(async (): Promise<HeadersInit> => {
    if (typeof window === "undefined") return {}
    try {
      const session = await getSession()
      console.log("🔐 Current session:", session) // ✅ Debug
      console.log("🔐 Access token:", session?.accessToken ? "EXISTS" : "MISSING") // ✅ Debug
      console.log("🔐 User:", session?.user) // ✅ Debug
      
      if (session?.error === "RefreshAccessTokenError") {
        await signOut({ callbackUrl: '/login' })
        throw new Error("Session expired. Please sign in again.")
      }
      const token = session?.accessToken
      
      const headers = {
        "Content-Type": "application/json",
        "Authorization": token ? `Bearer ${token}` : ""
      }
      
      console.log("🔐 Headers to send:", {
        "Content-Type": headers["Content-Type"],
        "Authorization": headers["Authorization"] ? `Bearer ${headers["Authorization"].substring(0, 20)}...` : "MISSING"
      }) // ✅ Debug
      
      return headers
    } catch (error) {
      console.error("❌ Error getting auth headers:", error)
      throw error
    }
  }, [])

  const handleSearchTariffs = async () => {
    setLoading(true)
    setError("")
    setSuccess("")
    setSearchResults([])

    try {
      const headers = await getAuthHeaders()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

      // ✅ Build search parameters from selected values
      const searchRequest = {
        reporterCode: selectedDestination, // ✅ Destination is the reporter
        partnerCode: selectedSource,       // ✅ Source is the partner
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

      // ✅ MAP BACKEND FIELDS TO FRONTEND INTERFACE
      const mappedResults: TariffData[] = (data.tariffs || []).map((tariff: any) => ({
        tariffId: tariff.tariffId,
        tariffYear: tariff.tariffYear || tariff.year,  // ✅ Handle both field names
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
      }))

      console.log("✅ Mapped results:", mappedResults)

      setSearchResults(mappedResults)
      setAvailableTariffs(mappedResults) // ✅ Also update availableTariffs for step 2
      setStep(2) // ✅ Move to step 2 to show results

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

  // ✅ FIXED: Fetch full tariff details when editing
  const handleEditTariff = async (tariff: TariffData) => {
    console.log("✏️ Opening edit dialog for tariff:", tariff.tariffId)
    console.log("📦 Full tariff data:", JSON.stringify(tariff, null, 2))
    
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
      // ✅ Only include if value exists (don't send 0 or undefined)
      ...(tariff.adValoremRate !== undefined && tariff.adValoremRate !== null && { adValoremRate: tariff.adValoremRate }),
      ...(tariff.specificRate !== undefined && tariff.specificRate !== null && { specificRate: tariff.specificRate }),
      ...(tariff.compoundRate1 !== undefined && tariff.compoundRate1 !== null && { compoundRate1: tariff.compoundRate1 }),
      ...(tariff.compoundRate2 !== undefined && tariff.compoundRate2 !== null && { compoundRate2: tariff.compoundRate2 }),
    }
    
    console.log("✅ Form data populated:", JSON.stringify(formData, null, 2))
    
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
      
      console.log("💾 Updating tariff:", selectedTariff.tariffId)
      
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"
      const endpoint = `${apiUrl}/api/admin/tariffs/${selectedTariff.tariffId}`
      
      // ✅ BUILD REQUEST BODY - ONLY include the fields that can be updated
      const requestBody: Record<string, any> = {
        tlsSuffix: editFormData.tlsSuffix || selectedTariff.tlsSuffix || "",
        note: editFormData.note || selectedTariff.note || "",
        specificRateUnit: editFormData.specificRateUnit || selectedTariff.specificRateUnit || "",
      }
      
      // ✅ FIX: Ensure numeric values are actually numbers, not strings
      const adValoremValue = editFormData.adValoremRate !== undefined 
        ? Number(editFormData.adValoremRate)  // ✅ Convert to number
        : (selectedTariff.adValoremRate !== undefined ? Number(selectedTariff.adValoremRate) : undefined)
    
      if (adValoremValue !== undefined && !isNaN(adValoremValue) && adValoremValue >= 0) {
        requestBody.adValoremRate = adValoremValue
      }
    
      const specificValue = editFormData.specificRate !== undefined 
        ? Number(editFormData.specificRate)  // ✅ Convert to number
        : (selectedTariff.specificRate !== undefined ? Number(selectedTariff.specificRate) : undefined)
    
      if (specificValue !== undefined && !isNaN(specificValue) && specificValue >= 0) {
        requestBody.specificRate = specificValue
      }
    
      const compound1Value = editFormData.compoundRate1 !== undefined 
        ? Number(editFormData.compoundRate1)  // ✅ Convert to number
        : (selectedTariff.compoundRate1 !== undefined ? Number(selectedTariff.compoundRate1) : undefined)
    
      if (compound1Value !== undefined && !isNaN(compound1Value) && compound1Value >= 0) {
        requestBody.compoundRate1 = compound1Value
      }
    
      const compound2Value = editFormData.compoundRate2 !== undefined 
        ? Number(editFormData.compoundRate2)  // ✅ Convert to number
        : (selectedTariff.compoundRate2 !== undefined ? Number(selectedTariff.compoundRate2) : undefined)
    
      if (compound2Value !== undefined && !isNaN(compound2Value) && compound2Value >= 0) {
        requestBody.compoundRate2 = compound2Value
      }
    
      console.log("📤 FINAL REQUEST BODY:", JSON.stringify(requestBody, null, 2))
      
      const response = await fetch(endpoint, {
        method: 'PUT',
        headers,
        body: JSON.stringify(requestBody),
        mode: 'cors',
        credentials: 'include',
      })

      console.log("📡 Response status:", response.status)
      
      if (!response.ok) {
        const errorText = await response.text()
        console.error("❌ Error response body:", errorText)
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
      
      // Refresh the search results
      await handleSearchTariffs()
      
    } catch (e) {
      const err = e as Error
      console.error("❌ Update failed:", err)
      
      setError(`Update failed: ${err.message}`)
      
      showNotification(
        'error',
        'Update Failed ❌',
        `Failed to update tariff ${selectedTariff.tariffId}`,
        err.message
      )
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteTariff = async (tariff: TariffData) => {
    if (!confirm(`Are you sure you want to delete tariff ${tariff.tariffId}?\n\nProduct: ${tariff.tlCode} - ${tariff.productDescription}\nRoute: ${tariff.partnerName} → ${tariff.reporterName}`)) return
    
    setLoading(true)
    try {
      const headers = await getAuthHeaders()
      
      console.log("🗑️ Deleting tariff:", tariff.tariffId)
      
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/admin/tariffs/${tariff.tariffId}`,
        {
          method: 'DELETE',
          headers,
          mode: 'cors',
          credentials: 'include',
        }
      )

      console.log("📡 Delete response status:", response.status)

      if (!response.ok) {
        throw new Error(`Delete failed (${response.status})`)
      }

      setSuccess("Tariff deleted successfully!")
      
      showNotification(
        'success',
        'Tariff Deleted!',
        `Tariff ID ${tariff.tariffId} has been permanently deleted`,
        `Product: ${tariff.tlCode} - ${tariff.productDescription}`
      )
      
      await handleSearchTariffs()
      
    } catch (e) {
      const err = e as Error
      console.error("❌ Delete failed:", err)
      setError(`Delete failed: ${err.message}`)
      
      showNotification(
        'error',
        'Delete Failed',
        `Failed to delete tariff ${tariff.tariffId}`,
        err.message
      )
    }
    setLoading(false)
  }

  return (
    <div className="h-full flex flex-col space-y-3 p-1">
      {step === 1 ? (
        // Step 1: Search Form
        <Card className="flex-1 bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 shadow-sm rounded-none">
          <CardHeader className="pb-0 px-4 pt-0">
            <CardTitle className="text-xl flex items-center gap-2 text-slate-900 dark:text-slate-100">
              <Search className="h-5.5 w-5.5 text-blue-600 dark:text-blue-400" />
              Find Tariffs to Edit
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 px-4 pb-4">
            {/* Source Country */}
            <div className="space-y-1.5">
              <Label htmlFor="source" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Source Country (Partner)
              </Label>
              <Select onValueChange={setSelectedSource}>
                <SelectTrigger className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100">
                  <SelectValue placeholder="Select source" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="702">702 - Singapore</SelectItem>
                  <SelectItem value="840">840 - United States</SelectItem>
                  <SelectItem value="156">156 - China</SelectItem>
                  <SelectItem value="000">000 - World (Any Country)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Destination Country */}
            <div className="space-y-1.5">
              <Label htmlFor="destination" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Destination Country (Reporter)
              </Label>
              <Select onValueChange={setSelectedDestination}>
                <SelectTrigger className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100">
                  <SelectValue placeholder="Select destination" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="702">702 - Singapore</SelectItem>
                  <SelectItem value="840">840 - United States</SelectItem>
                  <SelectItem value="156">156 - China</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Product Search */}
            <div className="space-y-1.5">
              <Label htmlFor="product" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Product Search
              </Label>
              <div className="relative">
                <Input
                  type="text"
                  value={productSearchQuery}
                  onChange={(e) => setProductSearchQuery(e.target.value)}
                  placeholder="Search by HS Code or description"
                  className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100"
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
            </div>

            {/* Year Selection */}
            <div className="space-y-1.5">
              <Label htmlFor="year" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Year
              </Label>
              <Select onValueChange={setSelectedYear} value={selectedYear}>
                <SelectTrigger className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100">
                  <SelectValue placeholder="Select year" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="2024">2024</SelectItem>
                  <SelectItem value="2023">2023</SelectItem>
                  <SelectItem value="2022">2022</SelectItem>
                  <SelectItem value="2021">2021</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button
              onClick={handleSearchTariffs}
              disabled={loading || !selectedProduct || !selectedSource || !selectedDestination}
              className="w-full h-9 mt-4 bg-blue-600 hover:bg-blue-700 text-white font-medium disabled:opacity-50"
            >
              <Search className="h-4 w-4 mr-2" />
              {loading ? "Searching..." : `Search Tariffs for ${selectedYear}`}
            </Button>
          </CardContent>
        </Card>
      ) : (
        // Step 2: Manage Tariffs
        <Card className="flex-1 bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 shadow-sm rounded-none">
          <CardHeader className="pb-0 px-4 pt-0">
            <CardTitle className="text-xl flex items-center gap-2 text-slate-900 dark:text-slate-100">
              <Edit className="h-5.5 w-5.5 text-green-600 dark:text-green-400" />
              Manage Tariffs ({availableTariffs.length} found)
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 px-4 pb-4">
            <Button
              variant="outline"
              onClick={() => setStep(1)}
              className="w-full h-9 border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-300"
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Search
            </Button>

            <div className="space-y-3 max-h-96 overflow-y-auto">
              {availableTariffs.map((tariff) => (
                <div
                  key={tariff.tariffId}
                  className="p-4 border border-slate-200 dark:border-slate-600 rounded-lg bg-slate-50 dark:bg-slate-700"
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="font-semibold text-blue-600 dark:text-blue-400">
                          Tariff ID: {tariff.tariffId}
                        </span>
                        <span className="text-sm bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 px-2 py-1 rounded">
                          {tariff.tariffYear}
                        </span>
                      </div>
                      <div className="text-sm space-y-1">
                        <div><strong>Route:</strong> {tariff.partnerName} → {tariff.reporterName}</div>
                        <div><strong>Product:</strong> {tariff.tlCode} - {tariff.productDescription}</div>
                        <div><strong>Duty:</strong> {tariff.dutyTypeDescription}</div>
                        {tariff.dutyCategory && (
                          <div><strong>Category:</strong> {tariff.dutyCategory}</div>
                        )}
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleEditTariff(tariff)}
                        disabled={loading}
                      >
                        <Edit className="h-4 w-4 mr-1" />
                        Edit
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleDeleteTariff(tariff)}
                        disabled={loading}
                        className="text-red-600 hover:text-red-700"
                      >
                        <Trash2 className="h-4 w-4 mr-1" />
                        Delete
                      </Button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* ✅ FIXED: Edit Dialog with Controlled Inputs */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto bg-white dark:bg-slate-900 border-2 border-slate-300 dark:border-slate-700">
          <DialogHeader className="pb-4 border-b border-slate-200 dark:border-slate-700">
            <DialogTitle className="text-2xl font-bold text-slate-900 dark:text-slate-100 flex items-center gap-2">
              <Edit className="h-6 w-6 text-blue-600 dark:text-blue-400" />
              Edit Tariff #{selectedTariff?.tariffId}
            </DialogTitle>
            <DialogDescription className="text-slate-600 dark:text-slate-400 mt-2">
              Update tariff information. <span className="font-semibold">Fields in gray are read-only</span> and cannot be changed.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-6 py-4">
            {/* Read-only Section */}
            <div className="space-y-3">
              <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide">
                Read-Only Information
              </h3>
              <div className="grid grid-cols-2 gap-4 p-4 bg-slate-50 dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700">
                <div className="space-y-1.5">
                  <Label className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Reporter Code</Label>
                  <Input 
                    value={editFormData.reporterCode} 
                    disabled 
                    className="bg-slate-100 dark:bg-slate-900 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 font-mono"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">{selectedTariff?.reporterName}</p>
                </div>
                <div className="space-y-1.5">
                  <Label className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Partner Code</Label>
                  <Input 
                    value={editFormData.partnerCode} 
                    disabled 
                    className="bg-slate-100 dark:bg-slate-900 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 font-mono"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">{selectedTariff?.partnerName}</p>
                </div>
                <div className="space-y-1.5">
                  <Label className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Product Code (HS)</Label>
                  <Input 
                    value={editFormData.tlCode} 
                    disabled 
                    className="bg-slate-100 dark:bg-slate-900 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 font-mono"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2">{selectedTariff?.productDescription}</p>
                </div>
                <div className="space-y-1.5">
                  <Label className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase">Duty Type</Label>
                  <Input 
                    value={`${editFormData.dutyType} - ${editFormData.dutyCode}`} 
                    disabled 
                    className="bg-slate-100 dark:bg-slate-900 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">{selectedTariff?.dutyTypeDescription}</p>
                </div>
              </div>
            </div>

            {/* Editable Section */}
            <div className="space-y-3">
              <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
                <span className="h-1 w-1 rounded-full bg-green-500"></span>
                Editable Fields
              </h3>
              
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    Tariff Year <span className="text-red-500">*</span>
                  </Label>
                  {/* ✅ FIXED: Convert to string for controlled input */}
                  <Input
                    type="number"
                    value={(editFormData.tariffYear || new Date().getFullYear()).toString()}
                    onChange={(e) => {
                      const value = e.target.value
                      const parsed = parseInt(value)
                      setEditFormData({
                        ...editFormData,
                        tariffYear: isNaN(parsed) ? new Date().getFullYear() : parsed
                      })
                    }}
                    className="border-2 border-blue-300 dark:border-blue-700 focus:border-blue-500"
                    min="2000"
                    max="2100"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Current: {selectedTariff?.tariffYear}
                  </p>
                </div>
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                    TLS Suffix
                  </Label>
                  <Input
                    value={editFormData.tlsSuffix}
                    onChange={(e) => setEditFormData({
                      ...editFormData,
                      tlsSuffix: e.target.value
                    })}
                    placeholder="Optional suffix"
                    className="border-2 border-blue-300 dark:border-blue-700 focus:border-blue-500"
                  />
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    Current: {selectedTariff?.tlsSuffix || <span className="italic">None</span>}
                  </p>
                </div>
              </div>
            </div>

            {/* Duty Rates Section */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
                  <span className="h-1 w-1 rounded-full bg-amber-500"></span>
                  Duty Rates
                </h3>
                {selectedTariff?.dutyCategory && (
                  <span className="text-xs font-semibold px-2 py-1 rounded-full bg-amber-100 dark:bg-amber-900 text-amber-800 dark:text-amber-200">
                    Category: {selectedTariff.dutyCategory.replace(/_/g, ' ')}
                  </span>
                )}
              </div>
              
              <div className="grid grid-cols-2 gap-4 p-4 bg-amber-50 dark:bg-amber-900/10 rounded-lg border border-amber-200 dark:border-amber-800">
                {/* Ad Valorem Rate */}
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
                    <span>Ad Valorem Rate (%)</span>
                    {selectedTariff?.adValoremRate !== undefined && selectedTariff?.adValoremRate !== null && (
                      <span className="text-xs font-normal text-amber-600 dark:text-amber-400">
                        Current: {selectedTariff.adValoremRate}%
                      </span>
                    )}
                  </Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={editFormData.adValoremRate !== undefined ? editFormData.adValoremRate.toString() : ""}
                    onChange={(e) => {
                      const value = e.target.value.trim()  // ✅ Add trim()
                      const newFormData = { ...editFormData }
                      
                      if (value === "" || value === "0") {  // ✅ Treat "0" as empty
                        delete newFormData.adValoremRate
                      } else {
                        const parsed = parseFloat(value)
                        if (!isNaN(parsed) && parsed > 0) {  // ✅ Only set if > 0
                          newFormData.adValoremRate = parsed
                        } else {
                          delete newFormData.adValoremRate
                        }
                      }
                      
                      setEditFormData(newFormData)
                    }}
                    placeholder={selectedTariff?.adValoremRate !== undefined && selectedTariff?.adValoremRate !== null ? `Currently ${selectedTariff.adValoremRate}%` : "e.g., 5.5"}
                    className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500"
                  />
                </div>

                {/* Specific Rate */}
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
                    <span>Specific Rate</span>
                    {selectedTariff?.specificRate !== undefined && selectedTariff?.specificRate !== null && (
                      <span className="text-xs font-normal text-amber-600 dark:text-amber-400">
                        Current: {selectedTariff.specificRate} {selectedTariff.specificRateUnit}
                      </span>
                    )}
                  </Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={editFormData.specificRate !== undefined ? editFormData.specificRate.toString() : ""}
                    onChange={(e) => {
                      const value = e.target.value.trim()  // ✅ Add trim()
                      const newFormData = { ...editFormData }
                      
                      if (value === "" || value === "0") {  // ✅ Treat "0" as empty
                        delete newFormData.specificRate
                      } else {
                        const parsed = parseFloat(value)
                        if (!isNaN(parsed) && parsed > 0) {  // ✅ Only set if > 0
                          newFormData.specificRate = parsed
                        } else {
                          delete newFormData.specificRate
                        }
                      }
                      
                      setEditFormData(newFormData)
                    }}
                    placeholder={selectedTariff?.specificRate !== undefined && selectedTariff?.specificRate !== null ? `Currently ${selectedTariff.specificRate}` : "e.g., 10.50"}
                    className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500"
                  />
                </div>
                
                {/* Compound Rate 1 */}
                <div className="space-y-1.5">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
                    <span>Compound Rate 1</span>
                    {selectedTariff?.compoundRate1 !== undefined && selectedTariff?.compoundRate1 !== null && (
                      <span className="text-xs font-normal text-amber-600 dark:text-amber-400">
                        Current: {selectedTariff.compoundRate1}
                      </span>
                    )}
                  </Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={editFormData.compoundRate1 !== undefined ? editFormData.compoundRate1.toString() : ""}
                    onChange={(e) => {
                      const value = e.target.value.trim()  // ✅ Add trim()
                      const newFormData = { ...editFormData }
                      
                      if (value === "" || value === "0") {  // ✅ Treat "0" as empty
                        delete newFormData.compoundRate1
                      } else {
                        const parsed = parseFloat(value)
                        if (!isNaN(parsed) && parsed > 0) {  // ✅ Only set if > 0
                          newFormData.compoundRate1 = parsed
                        } else {
                          delete newFormData.compoundRate1
                        }
                      }
                      
                      setEditFormData(newFormData)
                    }}
                    placeholder={selectedTariff?.compoundRate1 !== undefined && selectedTariff?.compoundRate1 !== null ? `Currently ${selectedTariff.compoundRate1}` : "First component"}
                    className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500"
                  />
                </div>
                
                {/* Compound Rate 2 */}
                <div className="space-y-1.5 col-span-2">
                  <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
                    <span>Compound Rate 2</span>
                    {selectedTariff?.compoundRate2 !== undefined && selectedTariff?.compoundRate2 !== null && (
                      <span className="text-xs font-normal text-amber-600 dark:text-amber-400">
                        Current: {selectedTariff.compoundRate2}
                      </span>
                    )}
                  </Label>
                  <Input
                    type="number"
                    step="0.01"
                    value={editFormData.compoundRate2 !== undefined ? editFormData.compoundRate2.toString() : ""}
                    onChange={(e) => {
                      const value = e.target.value.trim()  // ✅ Add trim()
                      const newFormData = { ...editFormData }
                      
                      if (value === "" || value === "0") {  // ✅ Treat "0" as empty
                        delete newFormData.compoundRate2
                      } else {
                        const parsed = parseFloat(value)
                        if (!isNaN(parsed) && parsed > 0) {  // ✅ Only set if > 0
                          newFormData.compoundRate2 = parsed
                        } else {
                          delete newFormData.compoundRate2
                        }
                      }
                      
                      setEditFormData(newFormData)
                    }}
                    placeholder={selectedTariff?.compoundRate2 !== undefined && selectedTariff?.compoundRate2 !== null ? `Currently ${selectedTariff.compoundRate2}` : "Second component"}
                    className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500"
                  />
                </div>
              </div>

              {/* Summary of Active Rates */}
              <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                <p className="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-2">📊 Active Duty Rates Summary:</p>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  {selectedTariff?.adValoremRate !== undefined && (
                    <div className="flex items-center gap-1">
                      <CheckCircle className="h-3 w-3 text-green-600" />
                      <span className="text-slate-700 dark:text-slate-300">Ad Valorem: <strong>{selectedTariff.adValoremRate}%</strong></span>
                    </div>
                  )}
                  {selectedTariff?.specificRate !== undefined && (
                    <div className="flex items-center gap-1">
                      <CheckCircle className="h-3 w-3 text-green-600" />
                      <span className="text-slate-700 dark:text-slate-300">Specific: <strong>{selectedTariff.specificRate} {selectedTariff.specificRateUnit}</strong></span>
                    </div>
                  )}
                  {selectedTariff?.compoundRate1 !== undefined && (
                    <div className="flex items-center gap-1">
                      <CheckCircle className="h-3 w-3 text-green-600" />
                      <span className="text-slate-700 dark:text-slate-300">Compound 1: <strong>{selectedTariff.compoundRate1}</strong></span>
                    </div>
                  )}
                  {selectedTariff?.compoundRate2 !== undefined && (
                    <div className="flex items-center gap-1">
                      <CheckCircle className="h-3 w-3 text-green-600" />
                      <span className="text-slate-700 dark:text-slate-300">Compound 2: <strong>{selectedTariff.compoundRate2}</strong></span>
                    </div>
                  )}
                  {selectedTariff?.adValoremRate === undefined && 
                   selectedTariff?.specificRate === undefined && 
                   selectedTariff?.compoundRate1 === undefined && 
                   selectedTariff?.compoundRate2 === undefined && (
                    <div className="col-span-2 flex items-center gap-1 text-slate-500">
                      <XCircle className="h-3 w-3" />
                      <span className="italic">No duty rates currently set</span>
                    </div>
                  )}
                </div>
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
                  value={editFormData.note}
                  onChange={(e) => setEditFormData({
                    ...editFormData,
                    note: e.target.value
                  })}
                  rows={4}
                  className="w-full p-3 border-2 border-purple-300 dark:border-purple-700 focus:border-purple-500 rounded-md bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 resize-none"
                  placeholder={selectedTariff?.note || "Add any additional notes or comments about this tariff..."}
                  maxLength={1000}
                />
                <div className="flex justify-between items-center text-xs text-slate-500 dark:text-slate-400">
                  <span>Current: {selectedTariff?.note ? `"${selectedTariff.note.substring(0, 50)}${selectedTariff.note.length > 50 ? '...' : ''}"` : <span className="italic">No notes</span>}</span>
                  <span>{editFormData.note.length} / 1000 characters</span>
                </div>
              </div>
            </div>
          </div>

          <DialogFooter className="pt-4 border-t border-slate-200 dark:border-slate-700 gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setIsEditDialogOpen(false)
                setSelectedTariff(null)
              }}
              disabled={loading}
              className="min-w-[120px] border-2 border-slate-300 dark:border-slate-600 hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              <X className="h-4 w-4 mr-2" />
              Cancel
            </Button>
            <Button
              type="button"
              onClick={handleSaveTariff}
              disabled={loading}
              className="min-w-[120px] bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800 text-white font-semibold shadow-lg"
            >
              {loading ? (
                <>
                  <span className="animate-spin mr-2">⏳</span>
                  Saving...
                </>
              ) : (
                <>
                  <CheckCircle className="h-4 w-4 mr-2" />
                  Save Changes
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
              : 'bg-red-50 dark:bg-red-900/30 border-red-300 dark:border-red-700'
          }`}>
            <div className="p-4">
              <div className="flex items-start justify-between mb-3">
                <div className="flex items-center gap-2">
                  {notification.type === 'success' ? (
                    <CheckCircle className="h-6 w-6 text-green-600 dark:text-green-400 flex-shrink-0" />
                  ) : (
                    <XCircle className="h-6 w-6 text-red-600 dark:text-red-400 flex-shrink-0" />
                  )}
                  <h3 className={`text-lg font-bold ${
                    notification.type === 'success' 
                      ? 'text-green-900 dark:text-green-100'
                      : 'text-red-900 dark:text-red-100'
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
                      : 'text-red-600 hover:text-red-700 hover:bg-red-100'
                  }`}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
              
              <p className={`text-sm mb-3 ${
                notification.type === 'success' 
                  ? 'text-green-800 dark:text-green-200'
                  : 'text-red-800 dark:text-red-200'
              }`}>
                {notification.message}
              </p>
              
              {notification.details && (
                <div className={`p-3 rounded text-xs mb-3 ${
                  notification.type === 'success' 
                    ? 'bg-green-100 dark:bg-green-800/50 text-green-800 dark:text-green-200'
                    : 'bg-red-100 dark:bg-red-800/50 text-red-800 dark:text-red-200'
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
                      : 'bg-red-600 hover:bg-red-700'
                  } text-white`}
                >
                  Got it
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Status Messages */}
      {(error || success) && (
        <div className={`flex items-start gap-2 p-3 rounded-lg text-sm font-medium ${success
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
    </div>
  )
}

