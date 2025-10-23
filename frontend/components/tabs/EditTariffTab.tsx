"use client"

import { useState, useEffect, useCallback } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Search, Edit, Plus, CheckCircle, XCircle, Trash2, X } from "lucide-react"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "../ui/dialog"
import { searchProducts as apiSearchProducts } from "@/lib/api"
import { getSession, signOut } from "next-auth/react"

interface Product {
  code: string
  tlCode?: string
  description?: string
  name?: string
  matchType?: string
}

interface TariffData {
  tariffId: number
  year: number
  reporterCode: string
  reporterName: string
  partnerCode: string
  partnerName: string
  tlCode: string
  productDescription: string
  dutyType: string
  dutyCode: string
  dutyTypeDescription: string
  tlsSuffix?: string
  note?: string
  dutyCategory?: string
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
  adValoremRate?: number
  specificRate?: number
  specificRateUnit?: string
  compoundRate1?: number
  compoundRate2?: number
}

interface NotificationPopup {
  show: boolean
  type: 'success' | 'error'
  title: string
  message: string
  details?: string
}

export default function EditTariffTab() {
  // Search state (same as CalculateTab)
  const [selectedProduct, setSelectedProduct] = useState<string>("")
  const [selectedSource, setSelectedSource] = useState<string>("")
  const [selectedDestination, setSelectedDestination] = useState<string>("")
  const [selectedYear, setSelectedYear] = useState<string>("2023")

  // Product search state (same as CalculateTab)
  const [productSearchQuery, setProductSearchQuery] = useState<string>("")
  const [productSearchResults, setProductSearchResults] = useState<Array<{ code: string, description: string, matchType?: string }>>([])
  const [searchTimeout, setSearchTimeout] = useState<NodeJS.Timeout | null>(null)

  // Tariff management state
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
  })

  // UI state
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [step, setStep] = useState(1) // 1 = search, 2 = manage

  // UPDATED: Notification popup state (only for success/error operations)
  const [notification, setNotification] = useState<NotificationPopup>({
    show: false,
    type: 'success',
    title: '',
    message: '',
    details: ''
  })

  // Predefined products (same as CalculateTab)
  const predefinedProducts = [
    { code: "27079940", description: "Carbazole, Energy" },
    { code: "1012100", description: "Pure Bred Breeding Horses" },
    { code: "29092000", description: "Cyclanic, Pharmaceutical" },
    { code: "74130000", description: "Copper Wire" }
  ]

  // UPDATED: Show notification popup (only for update/delete operations)
  const showNotification = (type: 'success' | 'error', title: string, message: string, details?: string) => {
    setNotification({
      show: true,
      type,
      title,
      message,
      details
    })
  }

  // Hide notification popup
  const hideNotification = () => {
    setNotification(prev => ({ ...prev, show: false }))
  }

  // Product search functionality (same as CalculateTab)
  const searchProducts = useCallback(async (query: string) => {
    try {
      const { ok, data } = await apiSearchProducts(query, 5)

      if (ok && data.products && Array.isArray(data.products)) {
        console.log(`🔍 Backend search found ${data.products.length} results using ${data.searchType} search`)
        return data.products.map((p: Product) => ({
          code: p.code || p.tlCode,
          description: p.description || p.name || "No description available",
          matchType: p.matchType
        }))
      }

      console.log('🔄 Falling back to predefined products')
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

  // Handle product search with debouncing (same as CalculateTab)
  useEffect(() => {
    if (searchTimeout) {
      clearTimeout(searchTimeout)
    }

    if (productSearchQuery.length > 0) {
      const timeout = setTimeout(async () => {
        const results = await searchProducts(productSearchQuery)
        setProductSearchResults(results)
      }, 300)

      setSearchTimeout(timeout)
    } else {
      setProductSearchResults([])
    }

    return () => {
      if (searchTimeout) {
        clearTimeout(searchTimeout)
      }
    }
  }, [productSearchQuery, searchProducts])

  // Handle product selection (same as CalculateTab)
  const handleProductSelect = (product: { code: string, description: string, matchType?: string }) => {
    setSelectedProduct(product.code)
    setProductSearchQuery(`${product.code} - ${product.description}`)
    setProductSearchResults([])
  }

  // Auth headers helper
  const getAuthHeaders = async (): Promise<HeadersInit> => {
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
      console.error("Error getting auth headers:", error)
      throw error
    }
  }

  // UPDATED: Search for tariffs (NO popup, just inline success message)
  const handleSearchTariffs = async () => {
    setLoading(true)
    setError("")
    setSuccess("")
    try {
      const headers = await getAuthHeaders()
      
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/admin/tariffs/search`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          reporterCode: selectedDestination,
          partnerCode: selectedSource,
          productCode: selectedProduct,
          year: parseInt(selectedYear),
        }),
        mode: 'cors',
        credentials: 'include',
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new Error(errorData.error || `Search failed (${response.status})`)
      }

      const data = await response.json()
      setAvailableTariffs(data.tariffs || [])
      setStep(2)
      setSuccess(`Found ${data.tariffs?.length || 0} tariff(s) for ${selectedYear}`)
      
      // NO popup for search results - just inline message
    } catch (e) {
      const error = e as Error
      setError(`Search failed: ${error.message}`)
      // NO popup for search errors - just inline message
    }
    setLoading(false)
  }

  // Load tariff details for editing
  const handleEditTariff = async (tariff: TariffData) => {
    setLoading(true)
    try {
      const headers = await getAuthHeaders()
      
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/admin/tariffs/${tariff.tariffId}`, {
        method: 'GET',
        headers,
        mode: 'cors',
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error(`Failed to load tariff details (${response.status})`)
      }

      const tariffDetails = await response.json()
      
      // Populate edit form with current data
      setEditFormData({
        tariffYear: tariffDetails.tariffYear,
        reporterCode: tariffDetails.reporterCode,
        partnerCode: tariffDetails.partnerCode,
        tlCode: tariffDetails.tlCode,
        dutyType: tariffDetails.dutyType,
        dutyCode: tariffDetails.dutyCode,
        tlsSuffix: tariffDetails.tlsSuffix || "",
        note: tariffDetails.note || "",
        adValoremRate: tariffDetails.adValoremRate,
        specificRate: tariffDetails.specificRate,
        specificRateUnit: tariffDetails.specificRateUnit || "",
        compoundRate1: tariffDetails.compoundRate1,
        compoundRate2: tariffDetails.compoundRate2,
      })
      
      setSelectedTariff(tariff)
      setIsEditDialogOpen(true)
    } catch (e) {
      const error = e as Error
      setError(`Failed to load tariff: ${error.message}`)
      // NO popup for load errors - just inline message
    }
    setLoading(false)
  }

  // UPDATED: Save tariff changes with notification popup
  const handleSaveTariff = async () => {
    if (!selectedTariff) return
    
    setLoading(true)
    try {
      const headers = await getAuthHeaders()
      
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/admin/tariffs/${selectedTariff.tariffId}`, {
        method: 'PUT',
        headers,
        body: JSON.stringify(editFormData),
        mode: 'cors',
        credentials: 'include',
      })

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new Error(errorData.message || `Update failed (${response.status})`)
      }

      const updatedTariff = await response.json()
      
      setSuccess("Tariff updated successfully!")
      setIsEditDialogOpen(false)
      setSelectedTariff(null)
      
      // Show SUCCESS popup
      showNotification(
        'success',
        'Tariff Updated!',
        `Tariff ID ${selectedTariff.tariffId} has been successfully updated`,
        `Product: ${editFormData.tlCode}\nRoute: ${editFormData.partnerCode} → ${editFormData.reporterCode}\nYear: ${editFormData.tariffYear}`
      )
      
      // Refresh the tariff list
      await handleSearchTariffs()
    } catch (e) {
      const error = e as Error
      setError(`Update failed: ${error.message}`)
      
      // Show ERROR popup
      showNotification(
        'error',
        'Update Failed',
        `Failed to update tariff ${selectedTariff.tariffId}`,
        error.message
      )
    }
    setLoading(false)
  }

  // UPDATED: Delete tariff with notification popup
  const handleDeleteTariff = async (tariff: TariffData) => {
    if (!confirm(`Are you sure you want to delete tariff ${tariff.tariffId}?\n\nProduct: ${tariff.tlCode} - ${tariff.productDescription}\nRoute: ${tariff.partnerName} → ${tariff.reporterName}`)) return
    
    setLoading(true)
    try {
      const headers = await getAuthHeaders()
      
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/admin/tariffs/${tariff.tariffId}`, {
        method: 'DELETE',
        headers,
        mode: 'cors',
        credentials: 'include',
      })

      if (!response.ok) {
        throw new Error(`Delete failed (${response.status})`)
      }

      setSuccess("Tariff deleted successfully!")
      
      // Show SUCCESS popup
      showNotification(
        'success',
        'Tariff Deleted!',
        `Tariff ID ${tariff.tariffId} has been permanently deleted`,
        `Product: ${tariff.tlCode} - ${tariff.productDescription}`
      )
      
      // Refresh the tariff list
      await handleSearchTariffs()
    } catch (e) {
      const error = e as Error
      setError(`Delete failed: ${error.message}`)
      
      // Show ERROR popup
      showNotification(
        'error',
        'Delete Failed',
        `Failed to delete tariff ${tariff.tariffId}`,
        error.message
      )
    }
    setLoading(false)
  }

  return (
    <div className="h-full flex flex-col space-y-3 p-1">
      {step === 1 ? (
        // Step 1: Search Form (same as CalculateTab)
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
                          {tariff.year}
                        </span>
                      </div>
                      <div className="text-sm space-y-1">
                        <div><strong>Route:</strong> {tariff.partnerName} → {tariff.reporterName}</div>
                        <div><strong>Product:</strong> {tariff.tlCode} - {tariff.productDescription}</div>
                        <div><strong>Duty:</strong> {tariff.dutyTypeDescription}</div>
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

      {/* Edit Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit Tariff {selectedTariff?.tariffId}</DialogTitle>
            <DialogDescription>
              Update the tariff information below.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {/* Basic fields */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Tariff Year</Label>
                <Input
                  type="number"
                  value={editFormData.tariffYear}
                  onChange={(e) => setEditFormData({
                    ...editFormData,
                    tariffYear: parseInt(e.target.value)
                  })}
                />
              </div>
              <div>
                <Label>TLS Suffix</Label>
                <Input
                  value={editFormData.tlsSuffix}
                  onChange={(e) => setEditFormData({
                    ...editFormData,
                    tlsSuffix: e.target.value
                  })}
                />
              </div>
            </div>

            {/* Duty rates */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Ad Valorem Rate (%)</Label>
                <Input
                  type="number"
                  step="0.01"
                  value={editFormData.adValoremRate ?? ""}
                  onChange={(e) => setEditFormData({
                    ...editFormData,
                    adValoremRate: e.target.value ? parseFloat(e.target.value) : undefined
                  })}
                />
              </div>
              <div>
                <Label>Specific Rate</Label>
                <Input
                  type="number"
                  step="0.01"
                  value={editFormData.specificRate ?? ""}
                  onChange={(e) => setEditFormData({
                    ...editFormData,
                    specificRate: e.target.value ? parseFloat(e.target.value) : undefined
                  })}
                />
              </div>
            </div>

            <div>
              <Label>Notes</Label>
              <textarea
                value={editFormData.note}
                onChange={(e) => setEditFormData({
                  ...editFormData,
                  note: e.target.value
                })}
                rows={3}
                className="w-full p-2 border border-slate-300 dark:border-slate-600 rounded-md bg-white dark:bg-slate-700 text-slate-900 dark:text-slate-100"
                placeholder="Additional notes..."
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsEditDialogOpen(false)}
              disabled={loading}
            >
              Cancel
            </Button>
            <Button
              onClick={handleSaveTariff}
              disabled={loading}
            >
              {loading ? "Saving..." : "Save Changes"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* UPDATED: Smaller Notification Popup (ONLY for update/delete operations) */}
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
                      ? 'text-green-600 hover:text-green-700 hover:bg-green-100 dark:text-green-400 dark:hover:text-green-300 dark:hover:bg-green-800'
                      : 'text-red-600 hover:text-red-700 hover:bg-red-100 dark:text-red-400 dark:hover:text-red-300 dark:hover:bg-red-800'
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
                      ? 'bg-green-600 hover:bg-green-700 text-white'
                      : 'bg-red-600 hover:bg-red-700 text-white'
                  }`}
                >
                  Got it
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* UPDATED: Status Messages (matching CalculateTab styling exactly) */}
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
