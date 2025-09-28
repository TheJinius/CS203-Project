"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Search, Calculator, CheckCircle, XCircle } from "lucide-react"
import { searchTariffs, calculateTariff, getExchangeRate } from "@/lib/api"

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
  const [selectedYear, setSelectedYear] = useState<string>("2023") // Add year state
  
  // Calculate state  
  const [availableTariffs, setAvailableTariffs] = useState<any[]>([])
  const [selectedTariff, setSelectedTariff] = useState<string>("")
  const [amountOfProduct, setAmountOfProduct] = useState<string>("")
  //const [currency, setCurrency] = useState<string>("USD")
  
  // Exchange rate and tariff result state
  const [exchangeRates, setExchangeRates] = useState<{ [key: string]: number }>({})
  const [baseTariffAmountUSD, setBaseTariffAmountUSD] = useState<number | null>(null)
  
  // UI state
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [step, setStep] = useState(1) // 1 = search, 2 = calculate

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
        year: parseInt(selectedYear), // Use dynamic year instead of hardcoded 2023
      })
      if (ok) {
        console.log(data);
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

      // Calculate tariff (backend now returns amount in USD)
      const { ok, data } = await calculateTariff({
        reporterCode: selectedDestination,
        partnerCode: selectedSource,
        productCode: selectedProduct,
        tariffId: parseInt(selectedTariff),
        amountOfProduct: parseFloat(amountOfProduct),
        currency: "USD", // Always request in USD from backend
      })
      
      if (ok) {
        const tariffAmountUSD = data.tariffAmount // Backend returns in USD
        setBaseTariffAmountUSD(tariffAmountUSD) // Store USD base amount
        
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
              <Label htmlFor="product" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Product Code
              </Label>
              <Select onValueChange={setSelectedProduct}>
                <SelectTrigger className="h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select product" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="27079940" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    27079940 - Carbazole, Energy
                  </SelectItem>
                  <SelectItem value="1012100" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    1012100 - Pure Bred Breeding Horses
                  </SelectItem>
                  <SelectItem value="29092000" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    29092000 - Cyclanic, Pharmaceutical
                  </SelectItem>
                  <SelectItem value="74130000" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    74130000 - Copper Wire
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            
            <div className="space-y-1.5">
              <Label htmlFor="source" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Source Country (Partner)
              </Label>
              <Select onValueChange={setSelectedSource}>
                <SelectTrigger className="h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select source" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="702" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    702 - Singapore
                  </SelectItem>
                  <SelectItem value="840" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    840 - United States
                  </SelectItem>
                  <SelectItem value="156" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    156 - China
                  </SelectItem>
                  <SelectItem value="000" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
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
                <SelectTrigger className="h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select destination" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="840" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    840 - United States
                  </SelectItem>
                  <SelectItem value="918" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    918 - European Union
                  </SelectItem>
                  <SelectItem value="392" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    392 - Japan
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            
            {/* Add Year Selection Dropdown */}
            <div className="space-y-1.5">
              <Label htmlFor="year" className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Year
              </Label>
              <Select onValueChange={setSelectedYear} value={selectedYear}>
                <SelectTrigger className="h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select year" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="2024" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    2024
                  </SelectItem>
                  <SelectItem value="2023" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    2023
                  </SelectItem>
                  <SelectItem value="2022" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    2022
                  </SelectItem>
                  <SelectItem value="2021" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
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
              className="w-full h-9 border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 hover:text-slate-900 dark:hover:text-slate-100 max-w-[410px]"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to Search
            </Button>
            
            <div className="space-y-1.5">
              <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Select Tariff
              </Label>
              <Select onValueChange={setSelectedTariff} value={selectedTariff}>
                <SelectTrigger className="h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Choose a tariff" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600 max-h-48 overflow-y-auto">
                  {availableTariffs.map(tariff => (
                    <SelectItem 
                      key={tariff.tariffId} 
                      value={tariff.tariffId.toString()}
                      className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20 text-sm"
                    >
                      <div className="flex flex-col gap-0.5 py-1">
                        <span className="font-medium">Tariff ID: {tariff.tariffId}</span>
                        {tariff.description && (
                          <span className="text-xs text-slate-500 dark:text-slate-400 truncate">
                            {tariff.description}
                          </span>
                        )}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1.5">
              <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Amount of Product
              </Label>
              <Input
                type="number"
                value={amountOfProduct}
                onChange={(e) => setAmountOfProduct(e.target.value)}
                placeholder="Enter quantity/amount (e.g., 1000)"
                step="0.01"
                min="0"
                className="h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-sm font-medium max-w-[295px]"
              />
            </div>

            <div className="space-y-1.5">
              <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                Currency
              </Label>
              <Select onValueChange={onCurrencyChange} value={currency}>
                <SelectTrigger className="h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
                  <SelectValue placeholder="Select currency" />
                </SelectTrigger>
                <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
                  <SelectItem value="SGD" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    SGD - Singapore Dollar
                  </SelectItem>
                  <SelectItem value="USD" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    USD - US Dollar
                  </SelectItem>
                  <SelectItem value="EUR" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    EUR - Euro
                  </SelectItem>
                  <SelectItem value="JPY" className="text-slate-900 dark:text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
                    JPY - Japanese Yen
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button 
              onClick={handleCalculate} 
              disabled={loading || !selectedTariff || !amountOfProduct}
              className="w-full h-9 mt-4 bg-green-600 hover:bg-green-700 dark:bg-green-600 dark:hover:bg-green-700 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed max-w-[410px]"
            >
              <Calculator className="h-4 w-4" />
              {loading ? "Calculating..." : "Calculate Tariff"}
            </Button>
          </CardContent>
        </Card>
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
    </div>
  )
}