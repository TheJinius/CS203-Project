"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Search, Calculator, Loader2, ClipboardCheck } from "lucide-react"
import { searchTariffs, calculateTariff, getExchangeRate, getOptimalRoutes, COUNTRY_COORDINATES } from "@/lib/api"

import { Tariff, CalculationDetails, ComplianceTask, COUNTRY_NAMES } from './types'
import { useProductSearch } from './hooks/useProductSearch'
import { useTariffHelpers } from './hooks/useTariffHelpers'
import { fetchComplianceData } from './utils/complianceService'
import { SearchForm } from './components/SearchForm'
import { TariffSelector } from './components/TariffSelector'
import { AmountInputFields } from './components/AmountInputFields'
import { StatusMessages } from './components/StatusMessages'
import { CalculationDetailsTab } from './components/CalculationDetailsTab'
import { ComplianceTab } from './components/ComplianceTab'

export interface CalculationHistory {
  id: string
  timestamp: Date
  sourceCountry: string
  destinationCountry: string
  productCode: string
  productDescription: string
  tariffAmount: number
  currency: string
  tariffId: number
  dutyType?: string
  rate?: number
  year: string
}

interface CalculateTabProps {
  onCalculationResult: (result: number | null) => void
  onRouteCalculated?: (geojson: Record<string, unknown>) => void
  currency: string
  onCurrencyChange: (currency: string) => void
  onSaveCalculation: (calculation: CalculationHistory) => void
}

export default function CalculateTab({ 
  onCalculationResult, 
  onRouteCalculated, 
  currency, 
  onCurrencyChange,
  onSaveCalculation
}: CalculateTabProps) {
  // Search state
  const [selectedSource, setSelectedSource] = useState<string>("")
  const [selectedDestination, setSelectedDestination] = useState<string>("")
  const [selectedYear, setSelectedYear] = useState<string>("2023")
  const [step, setStep] = useState(1)

  // Product search hook
  const {
    productSearchQuery,
    setProductSearchQuery,
    productSearchResults,
    selectedProduct,
    handleProductSelect,
    clearProductSelection,
    searchTimeout
  } = useProductSearch()

  // Calculate state
  const [availableTariffs, setAvailableTariffs] = useState<Tariff[]>([])
  const [selectedTariff, setSelectedTariff] = useState<string>("")
  const [amountOfProduct, setAmountOfProduct] = useState<string>("")
  const [productValueDollars, setProductValueDollars] = useState<string>("")
  const [productQuantity, setProductQuantity] = useState<string>("")

  // Results state
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
  const [activeDetailsTab, setActiveDetailsTab] = useState<'calculation' | 'compliance'>('calculation')

  // Helpers
  const { convertFromUSD, getLowestTariffId, getPriorityColor } = useTariffHelpers()

  // Auto-convert tariff amount when currency changes
  useEffect(() => {
    if (baseTariffAmountUSD !== null && Object.keys(exchangeRates).length > 0) {
      const convertedAmount = convertFromUSD(baseTariffAmountUSD, currency, exchangeRates)
      onCalculationResult(convertedAmount)
      setSuccess(`Tariff: ${currency} ${convertedAmount.toFixed(2)}`)
      setError("")
    }
  }, [currency, baseTariffAmountUSD, exchangeRates, onCalculationResult, convertFromUSD])

  const resetResults = () => {
    setBaseTariffAmountUSD(null)
    setCalculationDetails(null)
    setExchangeRates({})
    setComplianceTasks([])
    setComplianceError("")
    setComplianceLoading(false)
    setSuccess("")
    setError("")
    setAmountOfProduct("")
    setProductValueDollars("")
    setProductQuantity("")
    setSelectedTariff("")
    onCalculationResult(null)
    setActiveDetailsTab('calculation')
  }

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
        onRouteCalculated(data)
      } else {
        console.warn('⚠️ Failed to calculate routes:', data.error)
      }
    } catch (error) {
      console.error('❌ Error calculating routes:', error)
    }
  }

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

  const handleCalculate = async () => {
    setLoading(true)
    setError("")
    setSuccess("")
    try {
      const exchangeRateResponse = await getExchangeRate()
      let rates: { [key: string]: number } = {}
      if (exchangeRateResponse.ok && exchangeRateResponse.data.rates) {
        rates = exchangeRateResponse.data.rates
        setExchangeRates(rates)
      }

      const tariff = availableTariffs.find(t => t.tariffId.toString() === selectedTariff)
      const isCombinedDuty = tariff?.dutyClass === 'CombinedDuty'

      const { ok, data } = await calculateTariff({
        reporterCode: selectedDestination,
        partnerCode: selectedSource,
        productCode: selectedProduct,
        tariffId: parseInt(selectedTariff),
        amountOfProduct: isCombinedDuty ? parseFloat(productQuantity) : parseFloat(amountOfProduct),
        productValueDollars: isCombinedDuty ? parseFloat(productValueDollars) : undefined,
        currency: "USD",
      })

      if (ok) {
        const tariffAmountUSD = data.tariffAmount
        setBaseTariffAmountUSD(tariffAmountUSD)
        setCalculationDetails(data)

        const finalAmount = convertFromUSD(tariffAmountUSD, currency, rates)
        onCalculationResult(finalAmount)
        setSuccess(`Tariff: ${currency} ${finalAmount.toFixed(2)}`)

        // Save calculation to history
        const calculation: CalculationHistory = {
          id: `calc-${Date.now()}`,
          timestamp: new Date(),
          sourceCountry: COUNTRY_NAMES[selectedSource] || selectedSource,
          destinationCountry: COUNTRY_NAMES[selectedDestination] || selectedDestination,
          productCode: selectedProduct,
          productDescription: data.productDescription || 'N/A',
          tariffAmount: finalAmount,
          currency: currency,
          tariffId: parseInt(selectedTariff),
          dutyType: data.dutyType,
          rate: data.rate,
          year: selectedYear
        }
        onSaveCalculation(calculation)

        if (data.productDescription && selectedDestination) {
          await fetchComplianceData(
            selectedDestination, 
            data.productDescription,
            setComplianceTasks,
            setComplianceError,
            setComplianceLoading
          )
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

  const handleBackToSearch = () => {
    resetResults()
    setStep(1)
  }

  return (
    <div className="h-full flex flex-col space-y-3 p-1">
      {step === 1 ? (
        <Card className="flex-1 bg-white dark:bg-slate-800 border-slate-200 dark:border-slate-700 shadow-sm rounded-none">
          <CardHeader className="pb-0 px-4 pt-0">
            <CardTitle className="text-xl flex items-center gap-2 text-slate-900 dark:text-slate-100">
              <Search className="h-5.5 w-5.5 text-blue-600 dark:text-blue-400" />
              Find Tariffs
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 px-4 pb-4">
            <SearchForm
              selectedSource={selectedSource}
              setSelectedSource={setSelectedSource}
              selectedDestination={selectedDestination}
              setSelectedDestination={setSelectedDestination}
              productSearchQuery={productSearchQuery}
              setProductSearchQuery={setProductSearchQuery}
              productSearchResults={productSearchResults}
              selectedProduct={selectedProduct}
              handleProductSelect={handleProductSelect}
              clearProductSelection={clearProductSelection}
              searchTimeout={searchTimeout}
              selectedYear={selectedYear}
              setSelectedYear={setSelectedYear}
              loading={loading}
              onSearch={handleSearchTariffs}
            />
          </CardContent>
        </Card>
      ) : (
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

            <TariffSelector
              availableTariffs={availableTariffs}
              selectedTariff={selectedTariff}
              setSelectedTariff={setSelectedTariff}
              getLowestTariffId={() => getLowestTariffId(availableTariffs)}
            />

            <AmountInputFields
              selectedTariff={selectedTariff}
              availableTariffs={availableTariffs}
              amountOfProduct={amountOfProduct}
              setAmountOfProduct={setAmountOfProduct}
              productValueDollars={productValueDollars}
              setProductValueDollars={setProductValueDollars}
              productQuantity={productQuantity}
              setProductQuantity={setProductQuantity}
            />

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
                
                if (isCombinedDuty) {
                  return !productValueDollars || !productQuantity
                }
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

      <StatusMessages 
        error={error} 
        success={success} 
        complianceError={complianceError} 
      />

      {calculationDetails && success && (
        <Card className="bg-blue-50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800 shadow-sm">
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

          <CardContent className="space-y-3 px-4 pb-3 text-sm">
            {activeDetailsTab === 'calculation' ? (
              <CalculationDetailsTab
                calculationDetails={calculationDetails}
                currency={currency}
                baseTariffAmountUSD={baseTariffAmountUSD}
                convertFromUSD={convertFromUSD}
                exchangeRates={exchangeRates}
              />
            ) : (
              <ComplianceTab
                complianceTasks={complianceTasks}
                complianceLoading={complianceLoading}
                selectedDestination={selectedDestination}
                getPriorityColor={getPriorityColor}
              />
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
