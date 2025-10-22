"use client"

import { useState, useEffect, useCallback, useMemo } from "react"
import { getSession, signOut } from "next-auth/react"
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Loader2, Plus, Pencil, Trash2, Search, AlertCircle, CheckCircle2 } from "lucide-react"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../ui/dialog"
import {
  Alert,
  AlertDescription,
  AlertTitle,
} from "@/components/ui/alert"
import { searchProducts as apiSearchProducts } from "@/lib/api"

// ============================================================================
// Types & Interfaces
// ============================================================================

interface TariffData {
  tariffId?: number
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

interface TariffResponse extends TariffData {
  reporterName: string
  partnerName: string
  productDescription: string
  dutyTypeDescription: string
  dutyCategory: string
}

interface AlertState {
  show: boolean
  type: "success" | "error"
  message: string
}

interface Product {
  code: string
  tlCode?: string
  description?: string
  name?: string
  matchType?: string
}

// ============================================================================
// Constants - Outside component to prevent recreation
// ============================================================================

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"

const PREDEFINED_PRODUCTS = [
  { code: "27079940", description: "Carbazole, Energy" },
  { code: "1012100", description: "Pure Bred Breeding Horses" },
  { code: "29092000", description: "Cyclanic, Pharmaceutical" },
  { code: "74130000", description: "Copper Wire" }
] as const

const DEBOUNCE_DELAY = 300
const ALERT_DURATION = 5000

// ============================================================================
// Utility Functions - Outside component to prevent recreation
// ============================================================================

async function getAuthHeaders(): Promise<HeadersInit> {
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

function formatDutyRate(tariff: TariffResponse): string {
  const rates: string[] = []
  
  if (tariff.adValoremRate !== undefined && tariff.adValoremRate !== null) {
    rates.push(`${tariff.adValoremRate}%`)
  }
  
  if (tariff.specificRate !== undefined && tariff.specificRate !== null) {
    rates.push(`${tariff.specificRate} ${tariff.specificRateUnit || ""}`.trim())
  }
  
  if (tariff.compoundRate1 !== undefined && tariff.compoundRate1 !== null && 
      tariff.compoundRate2 !== undefined && tariff.compoundRate2 !== null) {
    rates.push(`${tariff.compoundRate1}% + ${tariff.compoundRate2}`)
  }
  
  return rates.join(" | ") || "N/A"
}

// ============================================================================
// Main Component
// ============================================================================

export default function EditTariffTab() {
  // ============================================================================
  // State Management
  // ============================================================================
  
  const [tariffs, setTariffs] = useState<TariffResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [editingTariff, setEditingTariff] = useState<TariffResponse | null>(null)
  const [searchTerm, setSearchTerm] = useState("")
  const [alert, setAlert] = useState<AlertState>({ 
    show: false, 
    type: "success", 
    message: "" 
  })
  
  // Product search state
  const [productSearchQuery, setProductSearchQuery] = useState("")
  const [productSearchResults, setProductSearchResults] = useState<
    Array<{ code: string; description: string; matchType?: string }>
  >([])
  
  // Form state - useMemo to prevent recreation on every render
  const initialFormState: TariffData = useMemo(() => ({
    tariffYear: new Date().getFullYear(),
    reporterCode: "",
    partnerCode: "",
    tlCode: "",
    dutyType: "",
    dutyCode: "",
    tlsSuffix: "",
    note: "",
    adValoremRate: undefined,
    specificRate: undefined,
    specificRateUnit: "",
    compoundRate1: undefined,
    compoundRate2: undefined,
  }), [])

  const [formData, setFormData] = useState<TariffData>(initialFormState)

  // ============================================================================
  // Product Search - SAME PATTERN AS CALCULATETAB
  // ============================================================================

  const searchProducts = useCallback(async (query: string) => {
    try {
      const { ok, data } = await apiSearchProducts(query, 5)

      if (ok && data.products && Array.isArray(data.products)) {
        console.log(`🔍 Backend search found ${data.products.length} results using ${data.searchType} search`)
        return data.products.map((p: Product) => ({
          code: p.code || p.tlCode || "",
          description: p.description || p.name || "No description available",
          matchType: p.matchType
        }))
      }

      console.log('🔄 Falling back to predefined products')
      const isNumericQuery = /^\d+$/.test(query)

      const filtered = PREDEFINED_PRODUCTS.filter(product =>
        product.code.toLowerCase().includes(query.toLowerCase()) ||
        product.description.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5)

      return filtered.map(product => ({
        ...product,
        matchType: isNumericQuery && product.code.includes(query) 
          ? 'contains_code' 
          : 'description_match'
      }))

    } catch (error) {
      console.error('Product search error:', error)
      const isNumericQuery = /^\d+$/.test(query)
      const filtered = PREDEFINED_PRODUCTS.filter(product =>
        product.code.toLowerCase().includes(query.toLowerCase()) ||
        product.description.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5)

      return filtered.map(product => ({
        ...product,
        matchType: isNumericQuery && product.code.includes(query) 
          ? 'contains_code' 
          : 'description_match'
      }))
    }
  }, []) // ✅ EMPTY DEPENDENCY ARRAY - NO INFINITE LOOP

  // Debounced product search - SAME AS CALCULATETAB
  useEffect(() => {
    let timeoutId: NodeJS.Timeout | null = null

    if (productSearchQuery.length > 0) {
      timeoutId = setTimeout(async () => {
        const results = await searchProducts(productSearchQuery)
        setProductSearchResults(results)
      }, DEBOUNCE_DELAY)
    } else {
      setProductSearchResults([])
    }

    return () => {
      if (timeoutId) {
        clearTimeout(timeoutId)
      }
    }
  }, [productSearchQuery, searchProducts]) // ✅ Stable dependencies

  const handleProductSelect = useCallback((
    product: { code: string; description: string; matchType?: string }
  ) => {
    setFormData(prev => ({ ...prev, tlCode: product.code }))
    setProductSearchQuery(`${product.code} - ${product.description}`)
    setProductSearchResults([])
  }, [])

  // ============================================================================
  // Tariff CRUD Operations
  // ============================================================================

const fetchTariffs = useCallback(async () => {
  setLoading(true)
  try {
    // ✅ Use the admin endpoint that actually exists
    const url = `${API_BASE_URL}/api/admin/tariffs`
    console.log("🔍 Fetching tariffs from:", url)
    
    const headers = await getAuthHeaders()
    console.log("🔑 Auth headers prepared")
    
    const response = await fetch(url, {
      method: 'GET',  // ✅ Simple GET request
      headers,
      mode: 'cors',
      credentials: 'include',
    })
    
    console.log("📡 Response status:", response.status)
    
    if (response.status === 401) {
      throw new Error("Unauthorized: Please log in again")
    }
    
    if (response.status === 403) {
      throw new Error("Forbidden: You don't have permission to access tariffs")
    }
    
    if (!response.ok) {
      const errorText = await response.text()
      console.error("❌ Error response:", errorText)
      throw new Error(`Failed to fetch tariffs (${response.status}): ${errorText}`)
    }
    
    const data: TariffResponse[] = await response.json()
    console.log("✅ Fetched tariffs:", data.length)
    
    setTariffs(data)
  } catch (error) {
    console.error("❌ Fetch error:", error)
    const errorMessage = error instanceof Error ? error.message : "Failed to fetch tariffs"
    setAlert({ show: true, type: "error", message: errorMessage })
    setTimeout(() => {
      setAlert({ show: false, type: "success", message: "" })
    }, ALERT_DURATION)
  } finally {
    setLoading(false)
  }
}, [])

  // Fetch on mount only
  useEffect(() => {
    void fetchTariffs()
  }, [fetchTariffs]) // ✅ fetchTariffs is stable, only runs once

  const showAlertMessage = useCallback((type: "success" | "error", message: string) => {
    setAlert({ show: true, type, message })
    const timeoutId = setTimeout(() => {
      setAlert({ show: false, type: "success", message: "" })
    }, ALERT_DURATION)
    
    return () => clearTimeout(timeoutId)
  }, [])

  const resetForm = useCallback(() => {
    setEditingTariff(null)
    setFormData(initialFormState)
    setProductSearchQuery("")
    setProductSearchResults([])
  }, [initialFormState])

  const handleSubmit = useCallback(async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setLoading(true)

    try {
      // Validate required fields
      if (!formData.tlCode || !formData.reporterCode || !formData.partnerCode) {
        throw new Error("Please fill in all required fields")
      }

      // Validate at least one duty rate
      const hasRate = formData.adValoremRate !== undefined ||
                     formData.specificRate !== undefined ||
                     (formData.compoundRate1 !== undefined && formData.compoundRate2 !== undefined)
      
      if (!hasRate) {
        throw new Error("Please provide at least one duty rate")
      }

      const url = editingTariff
        ? `${API_BASE_URL}/api/admin/tariffs/${editingTariff.tariffId}`
        : `${API_BASE_URL}/api/admin/tariffs`
      
      const method = editingTariff ? "PUT" : "POST"

      console.log(`📤 ${method} request to:`, url)
      console.log("📦 Payload:", JSON.stringify(formData, null, 2))

      const headers = await getAuthHeaders()

      const response = await fetch(url, {
        method,
        headers,
        body: JSON.stringify(formData),
        mode: 'cors',
        credentials: 'include',
      })

      console.log("📡 Response status:", response.status)

      if (response.status === 401) {
        throw new Error("Unauthorized: Please log in again")
      }
      
      if (response.status === 403) {
        throw new Error("Forbidden: You don't have permission to modify tariffs")
      }

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new Error(errorData.message || `Failed to save tariff (${response.status})`)
      }

      showAlertMessage("success", `Tariff ${editingTariff ? "updated" : "created"} successfully`)
      setIsFormOpen(false)
      resetForm()
      void fetchTariffs()
    } catch (error) {
      console.error("❌ Submit error:", error)
      const errorMessage = error instanceof Error ? error.message : "An error occurred"
      showAlertMessage("error", errorMessage)
    } finally {
      setLoading(false)
    }
  }, [formData, editingTariff, showAlertMessage, resetForm, fetchTariffs])

  const handleDelete = useCallback(async (id: number) => {
    if (typeof window === "undefined") return
    
    const confirmed = window.confirm(
      "Are you sure you want to delete this tariff? This action cannot be undone."
    )
    if (!confirmed) return

    setLoading(true)
    try {
      console.log("🗑️ Deleting tariff:", id)
      
      const headers = await getAuthHeaders()
      
      const response = await fetch(`${API_BASE_URL}/api/admin/tariffs/${id}`, {
        method: "DELETE",
        headers,
        mode: 'cors',
        credentials: 'include',
      })

      console.log("📡 Delete response status:", response.status)

      if (response.status === 401) {
        throw new Error("Unauthorized: Please log in again")
      }
      
      if (response.status === 403) {
        throw new Error("Forbidden: You don't have permission to delete tariffs")
      }

      if (!response.ok && response.status !== 204) {
        const errorText = await response.text()
        throw new Error(`Failed to delete tariff (${response.status}): ${errorText}`)
      }

      showAlertMessage("success", "Tariff deleted successfully")
      void fetchTariffs()
    } catch (error) {
      console.error("❌ Delete error:", error)
      const errorMessage = error instanceof Error ? error.message : "Failed to delete tariff"
      showAlertMessage("error", errorMessage)
    } finally {
      setLoading(false)
    }
  }, [showAlertMessage, fetchTariffs])

  const handleEdit = useCallback((tariff: TariffResponse) => {
    setEditingTariff(tariff)
    setFormData({
      tariffYear: tariff.tariffYear,
      reporterCode: tariff.reporterCode,
      partnerCode: tariff.partnerCode,
      tlCode: tariff.tlCode,
      dutyType: tariff.dutyType,
      dutyCode: tariff.dutyCode,
      tlsSuffix: tariff.tlsSuffix || "",
      note: tariff.note || "",
      adValoremRate: tariff.adValoremRate,
      specificRate: tariff.specificRate,
      specificRateUnit: tariff.specificRateUnit || "",
      compoundRate1: tariff.compoundRate1,
      compoundRate2: tariff.compoundRate2,
    })
    setProductSearchQuery(`${tariff.tlCode} - ${tariff.productDescription}`)
    setIsFormOpen(true)
  }, [])

  const handleFormChange = useCallback((
    field: keyof TariffData, 
    value: string | number | undefined
  ) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }, [])

  // ============================================================================
  // Memoized Values
  // ============================================================================

  const filteredTariffs = useMemo(() => {
    if (!searchTerm) return tariffs
    
    const search = searchTerm.toLowerCase()
    return tariffs.filter(tariff =>
      tariff.reporterName?.toLowerCase().includes(search) ||
      tariff.partnerName?.toLowerCase().includes(search) ||
      tariff.tlCode?.toLowerCase().includes(search) ||
      tariff.productDescription?.toLowerCase().includes(search)
    )
  }, [tariffs, searchTerm])

  // ============================================================================
  // Render
  // ============================================================================

  return (
    <div className="space-y-4">
      {/* Alert Banner */}
      {alert.show && (
        <Alert variant={alert.type === "error" ? "destructive" : "default"}>
          {alert.type === "success" ? (
            <CheckCircle2 className="h-4 w-4" />
          ) : (
            <AlertCircle className="h-4 w-4" />
          )}
          <AlertTitle>{alert.type === "success" ? "Success" : "Error"}</AlertTitle>
          <AlertDescription>{alert.message}</AlertDescription>
        </Alert>
      )}

      {/* Header Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Tariff Management</CardTitle>
            <Button 
              onClick={() => setIsFormOpen(true)} 
              size="sm"
              disabled={loading}
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Tariff
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2">
            <Search className="h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search by reporter, partner, product code, or description..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="max-w-lg"
            />
          </div>
        </CardContent>
      </Card>

      {/* Tariffs Table */}
      <Card>
        <CardContent className="pt-6">
          {loading && tariffs.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary mb-2" />
              <p className="text-sm text-muted-foreground">Loading tariffs...</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Year</TableHead>
                    <TableHead>Reporter</TableHead>
                    <TableHead>Partner</TableHead>
                    <TableHead>Product</TableHead>
                    <TableHead>Duty Type</TableHead>
                    <TableHead>Rate</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredTariffs.length === 0 ? (
                    <TableRow>
                      <TableCell 
                        colSpan={7} 
                        className="text-center py-12 text-muted-foreground"
                      >
                        {searchTerm ? (
                          <>No tariffs found matching &quot;{searchTerm}&quot;</>
                        ) : (
                          <>No tariffs available. Click &quot;Add Tariff&quot; to create one.</>
                        )}
                      </TableCell>
                    </TableRow>
                  ) : (
                    filteredTariffs.map((tariff) => (
                      <TableRow key={tariff.tariffId}>
                        <TableCell className="font-medium">
                          {tariff.tariffYear}
                        </TableCell>
                        <TableCell>{tariff.reporterName}</TableCell>
                        <TableCell>{tariff.partnerName}</TableCell>
                        <TableCell>
                          <div className="max-w-xs">
                            <div className="font-medium text-sm">{tariff.tlCode}</div>
                            <div className="text-xs text-muted-foreground line-clamp-2 mt-1">
                              {tariff.productDescription}
                            </div>
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="inline-flex items-center px-2 py-1 rounded-md text-xs font-medium bg-secondary">
                            {tariff.dutyCategory}
                          </span>
                        </TableCell>
                        <TableCell className="font-mono text-sm">
                          {formatDutyRate(tariff)}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleEdit(tariff)}
                              disabled={loading}
                              aria-label={`Edit tariff ${tariff.tariffId}`}
                              title="Edit tariff"
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => void handleDelete(tariff.tariffId!)}
                              disabled={loading}
                              aria-label={`Delete tariff ${tariff.tariffId}`}
                              title="Delete tariff"
                              className="text-destructive hover:text-destructive"
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add/Edit Dialog */}
      <Dialog
        open={isFormOpen}
        onOpenChange={(open: boolean) => {
          setIsFormOpen(open)
          if (!open) resetForm()
        }}
      >
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingTariff ? "Edit Tariff" : "Add New Tariff"}
            </DialogTitle>
            <DialogDescription>
              {editingTariff
                ? "Update the tariff information below. All fields marked with * are required."
                : "Fill in the details to create a new tariff. All fields marked with * are required."}
            </DialogDescription>
          </DialogHeader>

          <form 
            onSubmit={(e: React.FormEvent<HTMLFormElement>) => void handleSubmit(e)} 
            className="space-y-4"
          >
            {/* Basic Information */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="tariffYear">Tariff Year *</Label>
                <Input
                  id="tariffYear"
                  type="number"
                  value={formData.tariffYear}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                    handleFormChange("tariffYear", parseInt(e.target.value, 10))
                  }
                  required
                  min={2000}
                  max={2100}
                />
              </div>

              <div>
                <Label htmlFor="dutyType">Duty Type *</Label>
                <Input
                  id="dutyType"
                  value={formData.dutyType}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                    handleFormChange("dutyType", e.target.value)
                  }
                  placeholder="e.g., MFN"
                  required
                  maxLength={50}
                />
              </div>

              <div>
                <Label htmlFor="reporterCode">Reporter Country Code *</Label>
                <Input
                  id="reporterCode"
                  value={formData.reporterCode}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                    handleFormChange("reporterCode", e.target.value.toUpperCase())
                  }
                  placeholder="e.g., SGP"
                  maxLength={3}
                  required
                />
              </div>

              <div>
                <Label htmlFor="partnerCode">Partner Country Code *</Label>
                <Input
                  id="partnerCode"
                  value={formData.partnerCode}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                    handleFormChange("partnerCode", e.target.value.toUpperCase())
                  }
                  placeholder="e.g., USA"
                  maxLength={3}
                  required
                />
              </div>
            </div>

            {/* Product Search */}
            <div>
              <Label htmlFor="tlCode">Product TL Code *</Label>
              <div className="relative">
                <Input
                  id="tlCode"
                  value={productSearchQuery}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                    setProductSearchQuery(e.target.value)
                    const codeMatch = e.target.value.match(/^(\d+)/)
                    if (codeMatch) {
                      handleFormChange("tlCode", codeMatch[1])
                    }
                  }}
                  placeholder="Search by HS Code or description"
                  required
                  className="pr-10"
                />
                {productSearchQuery && (
                  <button
                    type="button"
                    onClick={() => {
                      setProductSearchQuery("")
                      handleFormChange("tlCode", "")
                      setProductSearchResults([])
                    }}
                    className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:text-slate-500 dark:hover:text-slate-300 transition-colors"
                    title="Clear selection"
                    aria-label="Clear product search"
                  >
                    ✕
                  </button>
                )}
                
                {/* Search Results Dropdown */}
                {productSearchResults.length > 0 && productSearchQuery && (
                  <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-md shadow-lg max-h-60 overflow-y-auto">
                    {productSearchResults.map((product, index) => (
                      <button
                        key={`${product.code}-${index}`}
                        type="button"
                        onClick={() => handleProductSelect(product)}
                        className="w-full text-left px-3 py-2 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-900 dark:text-slate-100 border-b border-slate-200 dark:border-slate-600 last:border-b-0 transition-colors"
                      >
                        <div className="font-medium text-blue-600 dark:text-blue-400">
                          {product.code}
                        </div>
                        <div className="text-sm text-slate-600 dark:text-slate-400 mt-1 line-clamp-2">
                          {product.description || "No description available"}
                        </div>
                      </button>
                    ))}
                  </div>
                )}
                
                {/* No Results Message */}
                {productSearchQuery && 
                 productSearchResults.length === 0 && (
                  <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-md shadow-lg p-3 text-center text-sm text-slate-500 dark:text-slate-400">
                    No products found matching &quot;{productSearchQuery}&quot;
                  </div>
                )}
              </div>
            </div>

            <div>
              <Label htmlFor="dutyCode">Duty Code *</Label>
              <Input
                id="dutyCode"
                value={formData.dutyCode}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  handleFormChange("dutyCode", e.target.value)
                }
                placeholder="e.g., AHS"
                required
                maxLength={50}
              />
            </div>

            <div>
              <Label htmlFor="tlsSuffix">TLS Suffix (Optional)</Label>
              <Input
                id="tlsSuffix"
                value={formData.tlsSuffix}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                  handleFormChange("tlsSuffix", e.target.value)
                }
                placeholder="Optional suffix"
                maxLength={20}
              />
            </div>

            {/* Duty Rates */}
            <div>
              <Label className="mb-2 block">
                Duty Rates <span className="text-sm text-muted-foreground">(provide at least one)</span>
              </Label>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="adValoremRate" className="text-sm">
                    Ad Valorem Rate (%)
                  </Label>
                  <Input
                    id="adValoremRate"
                    type="number"
                    step="0.01"
                    min={0}
                    max={100}
                    value={formData.adValoremRate ?? ""}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      handleFormChange("adValoremRate", e.target.value ? parseFloat(e.target.value) : undefined)
                    }
                    placeholder="e.g., 5.5"
                  />
                </div>

                <div>
                  <Label htmlFor="specificRate" className="text-sm">
                    Specific Rate
                  </Label>
                  <Input
                    id="specificRate"
                    type="number"
                    step="0.01"
                    min={0}
                    value={formData.specificRate ?? ""}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      handleFormChange("specificRate", e.target.value ? parseFloat(e.target.value) : undefined)
                    }
                    placeholder="e.g., 10.50"
                  />
                </div>

                <div>
                  <Label htmlFor="specificRateUnit" className="text-sm">
                    Specific Rate Unit
                  </Label>
                  <Input
                    id="specificRateUnit"
                    value={formData.specificRateUnit}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      handleFormChange("specificRateUnit", e.target.value)
                    }
                    placeholder="e.g., kg, m3"
                    maxLength={20}
                  />
                </div>

                <div>
                  <Label htmlFor="compoundRate1" className="text-sm">
                    Compound Rate 1 (%)
                  </Label>
                  <Input
                    id="compoundRate1"
                    type="number"
                    step="0.01"
                    min={0}
                    value={formData.compoundRate1 ?? ""}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      handleFormChange("compoundRate1", e.target.value ? parseFloat(e.target.value) : undefined)
                    }
                    placeholder="First rate"
                  />
                </div>

                <div className="col-span-2">
                  <Label htmlFor="compoundRate2" className="text-sm">
                    Compound Rate 2
                  </Label>
                  <Input
                    id="compoundRate2"
                    type="number"
                    step="0.01"
                    min={0}
                    value={formData.compoundRate2 ?? ""}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                      handleFormChange("compoundRate2", e.target.value ? parseFloat(e.target.value) : undefined)
                    }
                    placeholder="Second rate"
                  />
                </div>
              </div>
            </div>

            {/* Notes */}
            <div>
              <Label htmlFor="note">Additional Notes (Optional)</Label>
              <textarea
                id="note"
                value={formData.note}
                onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
                  handleFormChange("note", e.target.value)
                }
                placeholder="Add any additional information about this tariff..."
                rows={3}
                maxLength={1000}
                className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 resize-none"
              />
              <p className="text-xs text-muted-foreground mt-1">
                {formData.note?.length || 0}/1000 characters
              </p>
            </div>

            {/* Form Actions */}
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setIsFormOpen(false)
                  resetForm()
                }}
                disabled={loading}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={loading}>
                {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {editingTariff ? "Update" : "Create"} Tariff
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  )
}