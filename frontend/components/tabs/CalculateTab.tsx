"use client"

import { useState, useEffect, useCallback } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Search, Calculator, CheckCircle, XCircle } from "lucide-react"
import { searchTariffs, calculateTariff, getExchangeRate, searchProducts as apiSearchProducts } from "@/lib/api"

// Move predefined products outside component to prevent recreating on every render
const PREDEFINED_PRODUCTS = [
  { code: "27079940", description: "Carbazole, Energy" },
  { code: "1012100", description: "Pure Bred Breeding Horses" },
  { code: "29092000", description: "Cyclanic, Pharmaceutical" },
  { code: "74130000", description: "Copper Wire" }
]

interface Product {
  code: string
  tlCode?: string
  description?: string
  name?: string
  matchType?: string
}

interface Tariff {
  tariffId: number
  description?: string
  dutyType?: string
  dutyClass?: string  // Add this: Java class name like "AdValoremDuty", "SpecificDuty", etc.
  unit?: string
}

interface CalculationStep {
  step: string
  description: string
  value: string
}

interface CalculationDetails {
  tariffAmount: number
  currency: string
  tariffId: number
  status: string
  dutyType?: string
  dutyTypeCode?: string
  productDescription?: string
  productCode?: string
  formula?: string
  calculation?: string
  steps?: CalculationStep[]
  // Ad Valorem specific
  rate?: number
  rateDisplay?: string
  productValue?: number
  tariffResult?: number
  // Specific Duty specific
  amountPerUnit?: number
  amountPerUnitDisplay?: string
  multiplier?: number
  unit?: string
  productQuantity?: number
  billingUnits?: number
  billingUnitsDisplay?: string
  specificDutyRateRaw?: string
  // Combined Duty specific
  adValoremRate?: number
  adValoremRateDisplay?: string
  adValoremProductValue?: number
  adValoremAmount?: number
  specificAmountPerUnit?: number
  specificAmountPerUnitDisplay?: string
  specificMultiplier?: number
  specificUnit?: string
  specificProductQuantity?: number
  specificBillingUnits?: number
  specificBillingUnitsDisplay?: string
  specificAmount?: number
  mixedOrCompound?: string
  combinationType?: string
  combinationLogic?: string
  error?: string
}

interface CalculateTabProps {
  onCalculationResult: (result: number | null) => void
  currency: string
  onCurrencyChange: (currency: string) => void
}

export default function CalculateTab({ onCalculationResult, currency, onCurrencyChange }: CalculateTabProps) {
  // Search state
  const [selectedProduct, setSelectedProduct] = useState<string>("")
  const [selectedSource, setSelectedSource] = useState<string>("")
  const [selectedDestination, setSelectedDestination] = useState<string>("")
  const [selectedYear, setSelectedYear] = useState<string>("2023")

  // Product search state
  const [productSearchQuery, setProductSearchQuery] = useState<string>("")
  const [productSearchResults, setProductSearchResults] = useState<Array<{ code: string, description: string, matchType?: string }>>([])
  const [searchTimeout, setSearchTimeout] = useState<NodeJS.Timeout | null>(null)

  // Calculate state  
  const [availableTariffs, setAvailableTariffs] = useState<Tariff[]>([])
  const [selectedTariff, setSelectedTariff] = useState<string>("")
  const [amountOfProduct, setAmountOfProduct] = useState<string>("")
  const [productValueDollars, setProductValueDollars] = useState<string>("") // For Combined Duty Ad Valorem
  const [productQuantity, setProductQuantity] = useState<string>("") // For Combined Duty Specific

  // Exchange rate and tariff result state
  const [exchangeRates, setExchangeRates] = useState<{ [key: string]: number }>({})
  const [baseTariffAmountUSD, setBaseTariffAmountUSD] = useState<number | null>(null)
  const [calculationDetails, setCalculationDetails] = useState<CalculationDetails | null>(null)

  // UI state
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [step, setStep] = useState(1) // 1 = search, 2 = calculate

  // Product search API call with fallback to predefined products - wrapped in useCallback
  const searchProducts = useCallback(async (query: string) => {
    try {
      // Try backend API first
      const { ok, data } = await apiSearchProducts(query, 5)

      if (ok && data.products && Array.isArray(data.products)) {
        return data.products.map((p: Product) => ({
          code: p.code || p.tlCode,
          description: p.description || p.name || "No description available",
          matchType: p.matchType
        }))
      }

      // Fallback to predefined products if API fails or returns no results
      const isNumericQuery = /^\d+$/.test(query)

      const filtered = PREDEFINED_PRODUCTS.filter(product =>
        product.code.toLowerCase().includes(query.toLowerCase()) ||
        product.description.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5) // Limit to top 5 results

      // Add match type for predefined products
      return filtered.map(product => ({
        ...product,
        matchType: isNumericQuery && product.code.includes(query) ? 'contains_code' : 'description_match'
      }))

    } catch (error) {
      console.error('Product search error:', error)

      // Fallback to predefined products on error
      const isNumericQuery = /^\d+$/.test(query)
      const filtered = PREDEFINED_PRODUCTS.filter(product =>
        product.code.toLowerCase().includes(query.toLowerCase()) ||
        product.description.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5)

      return filtered.map(product => ({
        ...product,
        matchType: isNumericQuery && product.code.includes(query) ? 'contains_code' : 'description_match'
      }))
    }
  }, []) // Empty dependency array - PREDEFINED_PRODUCTS is constant at module level

  // Handle product search with debouncing
  useEffect(() => {
    if (searchTimeout) {
      clearTimeout(searchTimeout)
    }

    if (productSearchQuery.length > 0) {
      const timeout = setTimeout(async () => {
        const results = await searchProducts(productSearchQuery)
        setProductSearchResults(results)
      }, 300) // 300ms debounce

      setSearchTimeout(timeout)
    } else {
      setProductSearchResults([])
    }

    return () => {
      if (searchTimeout) {
        clearTimeout(searchTimeout)
      }
    }
  }, [productSearchQuery, searchProducts]) // Removed searchTimeout from dependencies

  // Helper function to get human-readable match type labels
  const getMatchTypeLabel = (matchType?: string) => {
    switch (matchType) {
      case 'exact_code': return '';
      case 'starts_with_code': return '';
      case 'contains_code': return '';
      case 'description_match': return '';
      default: return '';
    }
  }

  // Handle product selection from dropdown
  const handleProductSelect = (product: { code: string, description: string, matchType?: string }) => {
    setSelectedProduct(product.code)
    setProductSearchQuery(`${product.code} - ${product.description}`)
    setProductSearchResults([])
  }

  // Auto-convert tariff amount when currency changes
  useEffect(() => {
    if (baseTariffAmountUSD !== null && Object.keys(exchangeRates).length > 0) {
      const convertedAmount = convertFromUSD(baseTariffAmountUSD, currency, exchangeRates)
      onCalculationResult(convertedAmount)
      setSuccess(`Tariff: ${currency} ${convertedAmount.toFixed(2)}`)
      setError("")
    }
  }, [currency, baseTariffAmountUSD, exchangeRates, onCalculationResult])

  // Helper function to convert from USD to target currency
  const convertFromUSD = (amountUSD: number, targetCurrency: string, rates: { [key: string]: number }): number => {
    if (targetCurrency === "USD") return amountUSD
    const rate = rates[targetCurrency] || 1
    return Math.round(amountUSD * rate * 100) / 100
  }

  // Step 1: Search for available tariffs
  const handleSearchTariffs = async () => {
    setLoading(true)
    setError("")
    setSuccess("")
    setSelectedTariff("")
    try {
      const { ok, data } = await searchTariffs({
        reporter: selectedDestination,
        partner: selectedSource,
        tlCode: selectedProduct,
        year: parseInt(selectedYear),
      })
      if (ok) {
        setAvailableTariffs(data.tariffs || [])
        setStep(2)
        setSuccess(`Found ${data.tariffs?.length || 0} tariff(s) for ${selectedYear}`)
      } else {
        setError(data.error || 'Search failed')
      }
    } catch (e) {
      const error = e as Error
      setError(`Connection failed: ${error.message}`)
    }
    setLoading(false)
  }

  // Step 2: Calculate tariff with selected tariff
  const handleCalculate = async () => {
    setLoading(true)
    setError("")
    setSuccess("")
    try {
      // Get exchange rates (all rates are against USD base)
      const exchangeRateResponse = await getExchangeRate()
      let rates: { [key: string]: number } = {}
      if (exchangeRateResponse.ok && exchangeRateResponse.data.rates) {
        rates = exchangeRateResponse.data.rates
        setExchangeRates(rates) // Store exchange rates for currency conversion
      }

      // Get the selected tariff to check if it's Combined Duty
      const tariff = availableTariffs.find(t => t.tariffId.toString() === selectedTariff)
      const isCombinedDuty = tariff?.dutyClass === 'CombinedDuty'

      // Calculate tariff (backend now returns amount in USD)
      const { ok, data } = await calculateTariff({
        reporterCode: selectedDestination,
        partnerCode: selectedSource,
        productCode: selectedProduct,
        tariffId: parseInt(selectedTariff),
        amountOfProduct: isCombinedDuty ? parseFloat(productQuantity) : parseFloat(amountOfProduct),
        productValueDollars: isCombinedDuty ? parseFloat(productValueDollars) : undefined,
        currency: "USD", // Always request in USD from backend
      })

      if (ok) {
        const tariffAmountUSD = data.tariffAmount // Backend returns in USD
        setBaseTariffAmountUSD(tariffAmountUSD) // Store USD base amount
        
        setCalculationDetails(data) // Store entire response as calculation details

        // Convert to selected currency
        const finalAmount = convertFromUSD(tariffAmountUSD, currency, rates)

        onCalculationResult(finalAmount)
        setSuccess(`Tariff: ${currency} ${finalAmount.toFixed(2)}`)
      } else {
        setError(data.error || 'Calculation failed')
      }
    } catch (e) {
      const error = e as Error
      setError(`Connection failed: ${error.message}`)
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
              Find Tariffs
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 px-4 pb-4">
            <div className="space-y-1.5">
              <Label htmlFor="source" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Source Country (Partner)
              </Label>
              <Select onValueChange={setSelectedSource}>
                <SelectTrigger className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select source" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="702" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    702 - Singapore
                  </SelectItem>
                  <SelectItem value="840" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    840 - United States
                  </SelectItem>
                  <SelectItem value="156" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    156 - China
                  </SelectItem>
                  <SelectItem value="000" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    000 - World (Any Country)
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="destination" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Destination Country (Reporter)
              </Label>
              <Select onValueChange={setSelectedDestination}>
                <SelectTrigger className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select destination" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="702" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    702 - Singapore
                  </SelectItem>
                  <SelectItem value="840" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    840 - United States
                  </SelectItem>
                  <SelectItem value="156" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    156 - China
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

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
                  className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 pr-16"
                />
                {productSearchQuery && (
                  <>
                    {selectedProduct && (
                      <button
                        type="button"
                        onClick={() => {
                          setProductSearchQuery("")
                          setSelectedProduct("")
                          setProductSearchResults([])
                        }}
                        className="absolute right-2 top-2 text-slate-400 hover:text-slate-600 dark:text-slate-500 dark:hover:text-slate-300"
                        title="Clear selection"
                      >
                        ✕
                      </button>
                    )}
                  </>
                )}
                {productSearchResults.length > 0 && productSearchQuery && (
                  <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-md shadow-lg max-h-60 overflow-y-auto">
                    {productSearchResults.map((product, index) => (
                      <button
                        key={`${product.code}-${index}`}
                        type="button"
                        onClick={() => handleProductSelect(product)}
                        className="w-full text-left px-3 py-2 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-900 dark:text-slate-100 border-b border-slate-200 dark:border-slate-600 last:border-b-0"
                      >
                        <div className="flex items-center justify-between">
                          <div className="font-medium text-blue-600 dark:text-blue-400">{product.code}</div>
                          {product.matchType && (
                            <div className="text-xs px-2 py-1 rounded-full bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-400">
                              {getMatchTypeLabel(product.matchType)}
                            </div>
                          )}
                        </div>
                        <div className="text-sm text-slate-600 dark:text-slate-400 mt-1 line-clamp-2">
                          {product.description || "No description available"}
                        </div>
                      </button>
                    ))}
                  </div>
                )}
                {productSearchQuery && productSearchResults.length === 0 && searchTimeout === null && (
                  <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-md shadow-lg p-3 text-center text-sm text-slate-500 dark:text-slate-400">
                    No products found matching &quot;{productSearchQuery}&quot;
                  </div>
                )}
              </div>
            </div>

            {/* Add Year Selection Dropdown */}
            <div className="space-y-1.5">
              <Label htmlFor="year" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Year
              </Label>
              <Select onValueChange={setSelectedYear} value={selectedYear}>
                <SelectTrigger className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select year" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="2024" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    2024
                  </SelectItem>
                  <SelectItem value="2023" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    2023
                  </SelectItem>
                  <SelectItem value="2022" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    2022
                  </SelectItem>
                  <SelectItem value="2021" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    2021
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button
              onClick={handleSearchTariffs}
              disabled={loading || !selectedProduct || !selectedSource || !selectedDestination}
              className="w-full h-9 mt-4 bg-blue-600 hover:bg-blue-700 dark:bg-blue-600 dark:hover:bg-blue-700 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Search className="h-4 w-4" />
              {loading ? "Searching..." : `Search Available Tariffs for ${selectedYear}`}
            </Button>
          </CardContent>
        </Card>
      ) : (
        // Step 2: Calculate Form
        <Card className="flex-1 bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 shadow-sm rounded-none">
          <CardHeader className="pb-0 px-4 pt-0">
            <CardTitle className="text-xl flex items-center gap-2 text-slate-900 dark:text-slate-100">
              <Calculator className="h-5.5 w-5.5 text-green-600 dark:text-green-400" />
              Calculate Tariff
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 px-4 pb-4">
            <Button
              variant="outline"
              onClick={() => setStep(1)}
              className="w-full h-9 border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 hover:text-slate-900 dark:hover:text-slate-100"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to Search
            </Button>

            <div className="space-y-1.5">
              <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Select Tariff
              </Label>
              <Select onValueChange={setSelectedTariff} value={selectedTariff}>
                <SelectTrigger className="w-full min-h-25 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Choose a tariff" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600 max-h-60 overflow-y-auto [width:var(--radix-select-trigger-width)]">
                  {availableTariffs.map(tariff => (
                    <SelectItem
                      key={tariff.tariffId}
                      value={tariff.tariffId.toString()}
                      className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20 text-sm">
                      <div className="flex flex-col gap-0.5 py-1">
                        <span className="font-medium">Tariff ID: {tariff.tariffId}</span>
                        {tariff.description && (
                          <span className="text-xs text-slate-500 dark:text-slate-400 whitespace-normal">
                            {tariff.description}
                          </span>
                        )}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Conditional Input Fields based on Duty Type */}
            {selectedTariff && (() => {
              const tariff = availableTariffs.find(t => t.tariffId.toString() === selectedTariff)
              const isCombinedDuty = tariff?.dutyClass === 'CombinedDuty'
              
              if (isCombinedDuty) {
                // Show TWO input fields for Combined Duty
                return (
                  <>
                    <div className="space-y-1.5">
                      <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                        Product Value for Ad Valorem
                        <span className="text-blue-600 dark:text-blue-400"> (in dollars, $)</span>
                      </Label>
                      <Input
                        type="number"
                        value={productValueDollars}
                        onChange={(e) => setProductValueDollars(e.target.value)}
                        placeholder="Enter dollar value (e.g., 10000)"
                        step="0.01"
                        min="0"
                        className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-sm font-medium"
                      />
                    </div>
                    
                    <div className="space-y-1.5">
                      <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                        Product Quantity for Specific Duty
                        {tariff.unit && <span className="text-blue-600 dark:text-blue-400"> (in {tariff.unit})</span>}
                      </Label>
                      <Input
                        type="number"
                        value={productQuantity}
                        onChange={(e) => setProductQuantity(e.target.value)}
                        placeholder={`Enter quantity in ${tariff.unit || 'units'} (e.g., 1000)`}
                        step="0.01"
                        min="0"
                        className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-sm font-medium"
                      />
                    </div>
                  </>
                )
              } else {
                // Show single input field for Ad Valorem or Specific Duty
                return (
                  <div className="space-y-1.5">
                    <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                      Amount of Product
                      {(() => {
                        if (!tariff) return null
                        
                        // Check the Java class name directly
                        const isAdValorem = tariff.dutyClass === 'AdValoremDuty'
                        
                        if (isAdValorem) {
                          return <span className="text-blue-600 dark:text-blue-400"> (in dollars, $)</span>
                        } else if (tariff.unit) {
                          return <span className="text-blue-600 dark:text-blue-400"> (in {tariff.unit})</span>
                        }
                        return null
                      })()}
                    </Label>
                    <Input
                      type="number"
                      value={amountOfProduct}
                      onChange={(e) => setAmountOfProduct(e.target.value)}
                      placeholder={(() => {
                        if (!tariff) return "Enter quantity/amount (e.g., 1000)"
                        
                        // Check the Java class name directly
                        const isAdValorem = tariff.dutyClass === 'AdValoremDuty'
                        
                        if (isAdValorem) {
                          return "Enter dollar value (e.g., 10000)"
                        } else if (tariff.unit) {
                          return `Enter quantity in ${tariff.unit} (e.g., 1000)`
                        }
                        return "Enter quantity/amount (e.g., 1000)"
                      })()}
                      step="0.01"
                      min="0"
                      className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-sm font-medium"
                    />
                  </div>
                )
              }
            })()}

            <div className="space-y-1.5">
              <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Currency
              </Label>
              <Select onValueChange={onCurrencyChange} value={currency}>
                <SelectTrigger className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select currency" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="SGD" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    SGD - Singapore Dollar
                  </SelectItem>
                  <SelectItem value="USD" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    USD - US Dollar
                  </SelectItem>
                  <SelectItem value="EUR" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    EUR - Euro
                  </SelectItem>
                  <SelectItem value="JPY" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    JPY - Japanese Yen
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button
              onClick={handleCalculate}
              disabled={(() => {
                if (loading || !selectedTariff) return true
                const tariff = availableTariffs.find(t => t.tariffId.toString() === selectedTariff)
                const isCombinedDuty = tariff?.dutyClass === 'CombinedDuty'
                
                // For Combined Duty, need both fields filled
                if (isCombinedDuty) {
                  return !productValueDollars || !productQuantity
                }
                // For other duties, need single field filled
                return !amountOfProduct
              })()}
              className="w-full h-9 mt-4 bg-green-600 hover:bg-green-700 dark:bg-green-600 dark:hover:bg-green-700 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Calculator className="h-4 w-4" />
              {loading ? "Calculating..." : "Calculate Tariff"}
            </Button>
          </CardContent>
        </Card>
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

      {/* Calculation Details Card */}
      {calculationDetails && success && (
        <Card className="bg-blue-50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800 shadow-sm">
          <CardHeader className="pb-2 px-4 pt-3">
            <CardTitle className="text-base flex items-center gap-2 text-blue-900 dark:text-blue-100">
              <Calculator className="h-4 w-4 text-blue-600 dark:text-blue-400" />
              Calculation Logic
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 px-4 pb-3 text-sm">
            {/* Product Info */}
            {calculationDetails.productDescription && (
              <div className="p-2 bg-white dark:bg-slate-800 rounded border border-blue-100 dark:border-blue-900">
                <div className="font-medium text-slate-900 dark:text-slate-100 mb-1">Product</div>
                <div className="text-slate-600 dark:text-slate-400 text-xs">
                  {calculationDetails.productCode}: {calculationDetails.productDescription}
                </div>
              </div>
            )}

            {/* Duty Type */}
            {calculationDetails.dutyType && (
              <div className="p-2 bg-white dark:bg-slate-800 rounded border border-blue-100 dark:border-blue-900">
                <div className="font-medium text-slate-900 dark:text-slate-100 mb-1">Duty Type</div>
                <div className="text-slate-600 dark:text-slate-400 text-xs">
                  {calculationDetails.dutyType}
                  {calculationDetails.combinationType && (
                    <span className="ml-2 text-purple-600 dark:text-purple-400">
                      ({calculationDetails.combinationType})
                    </span>
                  )}
                </div>
              </div>
            )}

            {/* Formula */}
            {calculationDetails.formula && (
              <div className="p-2 bg-white dark:bg-slate-800 rounded border border-blue-100 dark:border-blue-900">
                <div className="font-medium text-slate-900 dark:text-slate-100 mb-1">Formula</div>
                <div className="text-blue-600 dark:text-blue-400 font-mono text-xs">
                  {calculationDetails.formula}
                </div>
                {calculationDetails.specificDutyRateRaw && (
                  <div className="mt-1 text-slate-600 dark:text-slate-400 text-xs">
                    <span className="font-medium">Raw Rate:</span> {calculationDetails.specificDutyRateRaw}
                  </div>
                )}
              </div>
            )}

            {/* Step-by-Step Calculation */}
            {calculationDetails.steps && calculationDetails.steps.length > 0 && (
              <div className="p-3 bg-gradient-to-br from-emerald-50 to-teal-50 dark:from-emerald-900/20 dark:to-teal-900/20 rounded border border-emerald-200 dark:border-emerald-800">
                <div className="font-medium text-slate-900 dark:text-slate-100 mb-2 flex items-center gap-2">
                  <Calculator className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                  Step-by-Step Calculation
                </div>
                <div className="space-y-2">
                  {calculationDetails.steps.map((step: CalculationStep, index: number) => (
                    <div 
                      key={index} 
                      className="flex items-start gap-2 p-2 bg-white dark:bg-slate-800 rounded border border-emerald-100 dark:border-emerald-900"
                    >
                      <div className="flex-shrink-0 w-6 h-6 rounded-full bg-emerald-600 dark:bg-emerald-700 text-white text-xs font-bold flex items-center justify-center">
                        {step.step}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-xs font-medium text-slate-700 dark:text-slate-300 mb-0.5">
                          {step.description}
                        </div>
                        <div className="text-xs font-mono text-emerald-700 dark:text-emerald-400 break-all">
                          {step.value}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Summary Calculation (one-liner) */}
            {calculationDetails.calculation && (
              <div className="p-2 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded border border-blue-200 dark:border-blue-800">
                <div className="font-medium text-slate-900 dark:text-slate-100 mb-1 text-xs">Full Calculation</div>
                <div className="text-blue-700 dark:text-blue-400 font-mono text-xs break-all">
                  {calculationDetails.calculation}
                </div>
              </div>
            )}

            {/* Final Result */}
            <div className="p-2 bg-gradient-to-r from-blue-100 to-purple-100 dark:from-blue-900/30 dark:to-purple-900/30 rounded border border-blue-300 dark:border-blue-700">
              <div className="font-medium text-slate-900 dark:text-slate-100 mb-1">Final Tariff</div>
              <div className="text-lg font-bold text-blue-600 dark:text-blue-400">
                {currency} {baseTariffAmountUSD !== null ? convertFromUSD(baseTariffAmountUSD, currency, exchangeRates).toFixed(2) : '0.00'}
              </div>
              {currency !== "USD" && baseTariffAmountUSD && (
                <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">
                  Base: USD ${baseTariffAmountUSD.toFixed(2)}
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}