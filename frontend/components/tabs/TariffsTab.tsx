"use client"

import { useState, useCallback, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Plus, CheckCircle, XCircle, AlertCircle } from "lucide-react"
import { getSession, signOut } from "next-auth/react"
import { searchProducts as apiSearchProducts } from "@/lib/api"

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
  }

  const handleCreateTariff = async () => {
    setLoading(true)
    setError("")
    setSuccess("")

    try {
      // Validation
      if (!tariffYear || !reporterCode || !partnerCode || !productCode || !dutyType || !dutyCode) {
        throw new Error("Please fill in all required fields (Year, Reporter, Partner, Product, Duty Type)")
      }

      // Validate at least one duty rate is provided
      const hasAdValorem = adValoremRate.trim() !== ""
      const hasSpecific = specificRate.trim() !== ""
      const hasCompound = compoundRate1.trim() !== "" || compoundRate2.trim() !== ""

      if (!hasAdValorem && !hasSpecific && !hasCompound) {
        throw new Error("Please provide at least one duty rate (Ad Valorem, Specific, or Compound)")
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

      // Add duty rates (only if they have values)
      if (adValoremRate.trim() !== "") {
        const value = parseFloat(adValoremRate)
        if (!isNaN(value) && value >= 0) {
          requestBody.adValoremRate = value
        }
      }

      if (specificRate.trim() !== "") {
        const value = parseFloat(specificRate)
        if (!isNaN(value) && value >= 0) {
          requestBody.specificRate = value
        }
      }

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

  return (
    <Card className="bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700">
      <CardHeader className="pb-4 border-b border-slate-200 dark:border-slate-700">
        <CardTitle className="text-lg flex items-center gap-2 text-slate-900 dark:text-slate-100">
          <Plus className="h-5 w-5 text-green-600 dark:text-green-400" />
          Add New Tariff
        </CardTitle>
        <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
          Create a new tariff entry in the database
        </p>
      </CardHeader>
      <CardContent className="space-y-4 pt-4">
        {/* Required Fields Section */}
        <div className="space-y-3">
          <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
            <span className="h-1 w-1 rounded-full bg-red-500"></span>
            Required Information
          </h3>
          
          <div className="grid grid-cols-2 gap-3">
            {/* Year */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Tariff Year <span className="text-red-500">*</span>
              </Label>
              <Input
                type="number"
                value={tariffYear}
                onChange={(e) => setTariffYear(e.target.value)}
                className="border-2 border-slate-300 dark:border-slate-600"
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
                <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600">
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
                <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600">
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
                <SelectTrigger className="border-2 border-slate-300 dark:border-slate-600">
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
            <div className="relative">
              <Input
                value={productSearchQuery}
                onChange={(e) => setProductSearchQuery(e.target.value)}
                placeholder="Search by HS Code or description"
                className="border-2 border-slate-300 dark:border-slate-600"
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
          
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                TLS Suffix
              </Label>
              <Input
                value={tlsSuffix}
                onChange={(e) => setTlsSuffix(e.target.value)}
                placeholder="e.g., A, B, 01"
                className="border-2 border-blue-300 dark:border-blue-700"
              />
            </div>

            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Specific Rate Unit
              </Label>
              <Input
                value={specificRateUnit}
                onChange={(e) => setSpecificRateUnit(e.target.value)}
                placeholder="e.g., kg, liter, unit"
                className="border-2 border-blue-300 dark:border-blue-700"
              />
            </div>
          </div>
        </div>

        {/* Duty Rates Section */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wide flex items-center gap-2">
              <span className="h-1 w-1 rounded-full bg-amber-500"></span>
              Duty Rates <span className="text-red-500 text-xs">(At least one required)</span>
            </h3>
          </div>
          
          <div className="grid grid-cols-2 gap-3 p-4 bg-amber-50 dark:bg-amber-900/10 rounded-lg border border-amber-200 dark:border-amber-800">
            {/* Ad Valorem Rate */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Ad Valorem Rate (%)
              </Label>
              <Input
                type="number"
                step="0.01"
                value={adValoremRate}
                onChange={(e) => setAdValoremRate(e.target.value)}
                placeholder="e.g., 5.5"
                className="border-2 border-amber-300 dark:border-amber-700"
              />
              <p className="text-xs text-amber-600 dark:text-amber-400">
                Percentage-based duty (0-100%)
              </p>
            </div>

            {/* Specific Rate */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Specific Rate
              </Label>
              <Input
                type="number"
                step="0.01"
                value={specificRate}
                onChange={(e) => setSpecificRate(e.target.value)}
                placeholder="e.g., 10.50"
                className="border-2 border-amber-300 dark:border-amber-700"
              />
              <p className="text-xs text-amber-600 dark:text-amber-400">
                Fixed amount per unit
              </p>
            </div>

            {/* Compound Rate 1 */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Compound Rate 1
              </Label>
              <Input
                type="number"
                step="0.01"
                value={compoundRate1}
                onChange={(e) => setCompoundRate1(e.target.value)}
                placeholder="First component"
                className="border-2 border-amber-300 dark:border-amber-700"
              />
              <p className="text-xs text-amber-600 dark:text-amber-400">
                Combined duty component 1
              </p>
            </div>

            {/* Compound Rate 2 */}
            <div className="space-y-1.5">
              <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Compound Rate 2
              </Label>
              <Input
                type="number"
                step="0.01"
                value={compoundRate2}
                onChange={(e) => setCompoundRate2(e.target.value)}
                placeholder="Second component"
                className="border-2 border-amber-300 dark:border-amber-700"
              />
              <p className="text-xs text-amber-600 dark:text-amber-400">
                Combined duty component 2
              </p>
            </div>
          </div>

          {/* Rate Type Indicator */}
          <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
            <p className="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-2">
              <AlertCircle className="h-3 w-3 inline mr-1" />
              Duty Rate Guidelines:
            </p>
            <ul className="text-xs text-slate-700 dark:text-slate-300 space-y-1 ml-4 list-disc">
              <li><strong>Ad Valorem:</strong> Use for percentage-based duties (e.g., 5% of value)</li>
              <li><strong>Specific:</strong> Use for fixed amount per unit (e.g., $10 per kg)</li>
              <li><strong>Compound:</strong> Use both components for combined duties (e.g., 5% + $2/kg)</li>
              <li><strong>Note:</strong> You can set rates to 0 if needed</li>
            </ul>
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
              className="w-full p-3 border-2 border-purple-300 dark:border-purple-700 rounded-md bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 resize-none"
              placeholder="Add any additional notes or comments about this tariff..."
              maxLength={1000}
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