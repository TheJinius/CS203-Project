"use client"

import { useState, useEffect, useCallback } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Search, Calculator, CheckCircle, XCircle, Loader2, AlertTriangle, ClipboardCheck,  } from "lucide-react"
import { searchTariffs, calculateTariff, getExchangeRate, searchProducts as apiSearchProducts, getShippingRoute, getOptimalRoutes, COUNTRY_COORDINATES } from "@/lib/api"

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
  rate?: number  // For ad valorem duties
  amountPerUnit?: number  // For specific duties
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
  onRouteCalculated?: (geojson: any) => void
  currency: string
  onCurrencyChange: (currency: string) => void
}

// Compliance checklist interface based on backend response
interface ComplianceTask {
  country: string
  sector: string
  task_category: string
  task_name: string
  description: string
  responsible_agency: string
  compliance_requirement: string
  timing: string
  reference: string
  reference_url: string
}

const COUNTRY_NAMES: { [key: string]: string } = {
  "702": "Singapore",
  "840": "United States",
  "156": "China",
  "000": "World (Any Country)"
}

export default function CalculateTab({ onCalculationResult, onRouteCalculated, currency, onCurrencyChange }: CalculateTabProps) {
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

  // Compliance state
  const [complianceTasks, setComplianceTasks] = useState<ComplianceTask[]>([])
  const [complianceLoading, setComplianceLoading] = useState(false)
  const [complianceError, setComplianceError] = useState("")

  // UI state
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [step, setStep] = useState(1) // 1 = search, 2 = calculate
  const [activeDetailsTab, setActiveDetailsTab] = useState<'calculation' | 'compliance'>('calculation') // New state for tabs

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

    } catch (error: unknown) {
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productSearchQuery, searchProducts]) // Intentionally excluding searchTimeout


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

  // Helper function to determine if a tariff is the lowest or a free trade agreement
  const getLowestTariffId = (): number | null => {
    if (availableTariffs.length === 0) return null
    
    // Check for free trade agreements (rate = 0 or description contains "free")
    const freeTradeTariff = availableTariffs.find(tariff => {
      const desc = tariff.description?.toLowerCase() || ''
      return desc.includes('0%') || desc.includes('free') || tariff.rate === 0
    })
    
    if (freeTradeTariff) return freeTradeTariff.tariffId
    
    // Otherwise, find the tariff with the lowest rate
    // For ad valorem duties, compare rates directly
    // For specific duties, we can't compare without quantity, so we'll prioritize ad valorem with lowest rate
    const adValoremTariffs = availableTariffs.filter(t => t.dutyClass === 'AdValoremDuty' && t.rate !== undefined)
    
    if (adValoremTariffs.length > 0) {
      const lowestAdValorem = adValoremTariffs.reduce((lowest, current) => {
        return (current.rate || Infinity) < (lowest.rate || Infinity) ? current : lowest
      })
      return lowestAdValorem.tariffId
    }
    
    // If no ad valorem tariffs, return the first tariff (we can't determine lowest for specific duties without quantity)
    return availableTariffs[0]?.tariffId || null
  }

  // Function to reset all calculation and compliance results
  const resetResults = () => {
    // Reset calculation results
    setBaseTariffAmountUSD(null)
    setCalculationDetails(null)
    setExchangeRates({})
    
    // Reset compliance results  
    setComplianceTasks([])
    setComplianceError("")
    setComplianceLoading(false)
    
    // Reset status messages
    setSuccess("")
    setError("")
    
    // Reset form values
    setAmountOfProduct("")
    setProductValueDollars("")
    setProductQuantity("")
    setSelectedTariff("")
    
    // Reset calculation result in parent
    onCalculationResult(null)
    
    // Reset to calculation tab
    setActiveDetailsTab('calculation')
  }

  // Function to fetch compliance data from backend
  const fetchComplianceData = async (destination: string, productDescription: string) => {
    setComplianceLoading(true)
    setComplianceError("")
    
    try {
      const countryName = COUNTRY_NAMES[destination] || destination
      const query = `${countryName} ${productDescription}`
      
      const response = await fetch("http://127.0.0.1:8001/query", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question: query }),
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const data = await response.json()
      
      // Parse the response - it should be a JSON array of compliance tasks
      let tasks: ComplianceTask[] = []
      try {
        // Clean the response string by removing markdown code blocks
        let responseStr = data.response
        if (typeof responseStr === 'string') {
          // Remove markdown code block syntax
          responseStr = responseStr.replace(/```json\s*/g, '').replace(/```\s*/g, '').trim()
          
          const parsedResponse = JSON.parse(responseStr)
          if (Array.isArray(parsedResponse)) {
            tasks = parsedResponse
          } else {
            console.warn("Backend response is not an array:", parsedResponse)
          }
        }
      } catch (parseError) {
        console.error("Failed to parse compliance response:", parseError)
        console.error("Raw response:", data.response)
        setComplianceError("Failed to parse compliance data")
      }

      setComplianceTasks(tasks)
      
      // REMOVED: Do not auto-switch to compliance tab
      // Keep user on their current tab
      
    } catch (err) {
      console.error("Compliance fetch error:", err)
      setComplianceError("Failed to fetch compliance data. Please make sure the Python backend is running on http://127.0.0.1:8001")
    } finally {
      setComplianceLoading(false)
    }
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
        
        // Calculate and display shipping route
        if (onRouteCalculated && selectedSource && selectedDestination) {
          calculateShippingRoute(selectedSource, selectedDestination)
        }
      } else {
        setError(data.error || 'Search failed')
      }
    } catch (e) {
      const error = e as Error
      setError(`Connection failed: ${error.message}`)
    }
    setLoading(false)
  }

  // Calculate shipping route between source and destination
  const calculateShippingRoute = async (sourceCode: string, destCode: string) => {
    const sourceCoords = COUNTRY_COORDINATES[sourceCode]
    const destCoords = COUNTRY_COORDINATES[destCode]
    
    if (!sourceCoords || !destCoords) {
      console.warn('⚠️ No coordinates found for selected countries')
      return
    }

    console.log(`🚢 Calculating optimal routes: ${sourceCoords.name} → ${destCoords.name}`)
    
    try {
      const { ok, data } = await getOptimalRoutes({
        src_lat: sourceCoords.lat,
        src_lon: sourceCoords.lon,
        dst_lat: destCoords.lat,
        dst_lon: destCoords.lon,
      })

      if (ok && onRouteCalculated) {
        console.log('✅ Optimal routes calculated successfully')
        // Transform the response to match the expected format
        onRouteCalculated(data)
      } else {
        console.warn('⚠️ Failed to calculate routes:', data.error)
      }
    } catch (error) {
      console.error('❌ Error calculating routes:', error)
    }
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

        // Fetch compliance data after successful calculation
        if (data.productDescription && selectedDestination) {
          await fetchComplianceData(selectedDestination, data.productDescription)
        }
      } else {
        setError(data.error || 'Calculation failed')
      }
    } catch (e) {
      const error = e as Error
      setError(`Connection failed: ${error.message}`)
    }
    setLoading(false)
  }

  // Handle back to search - reset all results
  const handleBackToSearch = () => {
    resetResults()
    setStep(1)
  }

  // Helper function to get priority color
  const getPriorityColor = (category: string) => {
    const lowerCategory = category.toLowerCase()
    if (lowerCategory.includes('high') || lowerCategory.includes('critical') || lowerCategory.includes('mandatory')) {
      return 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400'
    } else if (lowerCategory.includes('medium') || lowerCategory.includes('important')) {
      return 'bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400'
    } else {
      return 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
    }
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
                          {product.matchType}
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
              onClick={handleBackToSearch}
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
                  {availableTariffs.map(tariff => {
                    const lowestTariffId = getLowestTariffId()
                    const isLowest = tariff.tariffId === lowestTariffId
                    const desc = tariff.description?.toLowerCase() || ''
                    const isFTA = desc.includes('0%') || desc.includes('free') || tariff.rate === 0
                    
                    return (
                      <SelectItem
                        key={tariff.tariffId}
                        value={tariff.tariffId.toString()}
                        className={`!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20 text-sm ${
                          isLowest ? 'border-2 border-green-500 dark:border-green-400 bg-green-50 dark:bg-green-900/20 hover:!bg-green-100 dark:hover:!bg-green-900/30' : ''
                        }`}>
                        <div className="flex flex-col gap-0.5 py-1">
                          <div className="flex items-center gap-2">
                            <span className="font-medium">Tariff ID: {tariff.tariffId}</span>
                            {isLowest && isFTA && (
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-green-600 dark:bg-green-500 text-white">
                                FREE TRADE
                              </span>
                            )}
                            {isLowest && !isFTA && (
                              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-600 dark:bg-emerald-500 text-white">
                                LOWEST RATE
                              </span>
                            )}
                          </div>
                          {tariff.description && (
                            <span className="text-xs text-slate-500 dark:text-slate-400 whitespace-normal">
                              {tariff.description}
                            </span>
                          )}
                        </div>
                      </SelectItem>
                    )
                  })}
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
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Calculating...
                </>
              ) : (
                <>
                  <Calculator className="h-4 w-4" />
                  Calculate Tariff
                </>
              )}
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

      {/* Compliance Error Message */}
      {complianceError && (
        <div className="flex items-start gap-2 p-3 rounded-lg text-sm font-medium bg-orange-50 dark:bg-orange-900/20 text-orange-700 dark:text-orange-300 border border-orange-200 dark:border-orange-800">
          <AlertTriangle className="h-4 w-4 mt-0.5 text-orange-600 dark:text-orange-400 flex-shrink-0" />
          <span className="flex-1 break-words">{complianceError}</span>
        </div>
      )}

      {/* Tabbed Details Section */}
      {calculationDetails && success && (
        <Card className="bg-blue-50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800 shadow-sm">
          {/* Tab Headers */}
          <div className="flex border-b border-blue-200 dark:border-blue-800 bg-blue-100/50 dark:bg-blue-900/20">
            <button
              onClick={() => setActiveDetailsTab('calculation')}
              className={`flex-1 px-4 py-2 text-sm font-medium transition-colors ${
                activeDetailsTab === 'calculation'
                  ? 'bg-white dark:bg-slate-800 text-blue-900 dark:text-blue-100 border-b-2 border-blue-600 dark:border-blue-400'
                  : 'text-blue-700 dark:text-blue-300 hover:text-blue-900 dark:hover:text-blue-100 hover:bg-blue-100 dark:hover:bg-blue-900/30'
              }`}
            >
              <div className="flex items-center justify-center gap-2">
                <Calculator className="h-4 w-4" />
                Calculation Logic
              </div>
            </button>
            <button
              onClick={() => setActiveDetailsTab('compliance')}
              className={`flex-1 px-4 py-2 text-sm font-medium transition-colors ${
                activeDetailsTab === 'compliance'
                  ? 'bg-white dark:bg-slate-800 text-blue-900 dark:text-blue-100 border-b-2 border-blue-600 dark:border-blue-400'
                  : 'text-blue-700 dark:text-blue-300 hover:text-blue-900 dark:hover:text-blue-100 hover:bg-blue-100 dark:hover:bg-blue-900/30'
              }`}
            >
              <div className="flex items-center justify-center gap-2">
                <ClipboardCheck className="h-4 w-4" />
                Compliance {complianceLoading && <Loader2 className="h-3 w-3 animate-spin ml-1" />}
              </div>
            </button>
          </div>

          {/* Tab Content */}
          <CardContent className="space-y-3 px-4 pb-3 text-sm">
            {activeDetailsTab === 'calculation' ? (
              // Calculation Logic Tab Content (existing logic)
              <>
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
              </>
            ) : (

              <>
                {/* Compliance Checklist from Backend */}
                <div className="p-3 bg-gradient-to-br from-green-50 to-emerald-50 dark:from-green-900/20 dark:to-emerald-900/20 rounded border border-green-200 dark:border-green-800">
                  <div className="font-medium text-slate-900 dark:text-slate-100 mb-3 flex items-center gap-2">
                    <ClipboardCheck className="h-4 w-4 text-green-600 dark:text-green-400" />
                    Compliance Requirements for {COUNTRY_NAMES[selectedDestination] || selectedDestination}
                    {complianceLoading && <Loader2 className="h-4 w-4 animate-spin text-green-600 dark:text-green-400" />}
                  </div>
                  
                  {complianceLoading ? (
                    <div className="flex items-center justify-center py-8">
                      <Loader2 className="h-6 w-6 animate-spin text-green-600 dark:text-green-400" />
                      <span className="ml-2 text-sm text-slate-600 dark:text-slate-400">
                        Fetching compliance requirements...
                      </span>
                    </div>
                  ) : complianceTasks.length > 0 ? (
                    <div className="space-y-3">
                      {complianceTasks.map((task, index) => (
                        <div 
                          key={index} 
                          className="p-3 bg-white dark:bg-slate-800 rounded border border-green-100 dark:border-green-900"
                        >
                          <div className="flex items-start justify-between gap-2 mb-2">
                            <div className="flex-1 min-w-0">
                              <div className="text-sm font-medium text-slate-900 dark:text-slate-100">
                                {task.task_name}
                              </div>
                              <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">
                                {task.description}
                              </div>
                            </div>
                            <div className={`flex-shrink-0 px-2 py-1 rounded-full text-xs font-medium ${getPriorityColor(task.task_category)}`}>
                              {task.task_category}
                            </div>
                          </div>
                          
                          <div className="grid grid-cols-2 gap-2 text-xs mt-2">
                            <div>
                              <span className="text-slate-500 dark:text-slate-400">Agency:</span>
                              <span className="ml-1 text-slate-700 dark:text-slate-300">{task.responsible_agency}</span>
                            </div>
                            <div>
                              <span className="text-slate-500 dark:text-slate-400">Timing:</span>
                              <span className="ml-1 text-slate-700 dark:text-slate-300">{task.timing}</span>
                            </div>
                            <div className="col-span-2">
                              <span className="text-slate-500 dark:text-slate-400">Requirement:</span>
                              <span className="ml-1 text-slate-700 dark:text-slate-300">{task.compliance_requirement}</span>
                            </div>
                            {task.reference && (
                              <div className="col-span-2">
                                <span className="text-slate-500 dark:text-slate-400">Reference:</span>
                                {task.reference_url ? (
                                  <a 
                                    href={task.reference_url} 
                                    target="_blank" 
                                    rel="noopener noreferrer"
                                    className="ml-1 text-blue-600 dark:text-blue-400 hover:underline"
                                  >
                                    {task.reference}
                                  </a>
                                ) : (
                                  <span className="ml-1 text-slate-700 dark:text-slate-300">{task.reference}</span>
                                )}
                              </div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-6">
                      <div className="text-sm text-slate-600 dark:text-slate-400">
                        No specific compliance requirements found for this product and destination.
                      </div>
                      <div className="text-xs text-slate-500 dark:text-slate-500 mt-1">
                        This could mean standard import procedures apply, or the compliance database may not have specific rules for this combination.
                      </div>
                    </div>
                  )}
                </div>
              </>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}