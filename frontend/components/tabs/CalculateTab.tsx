"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { ArrowLeft, Search, Calculator } from "lucide-react"

interface CalculateTabProps {
  onCalculationResult: (result: number | null) => void
}

export default function CalculateTab({ onCalculationResult }: CalculateTabProps) {
  // Search state
  const [selectedProduct, setSelectedProduct] = useState<string>("")
  const [selectedSource, setSelectedSource] = useState<string>("")
  const [selectedDestination, setSelectedDestination] = useState<string>("")
  
  // Calculate state  
  const [availableTariffs, setAvailableTariffs] = useState<any[]>([])
  const [selectedTariff, setSelectedTariff] = useState<string>("")
  const [amountOfProduct, setAmountOfProduct] = useState<string>("")
  const [currency, setCurrency] = useState<string>("SGD")
  
  // UI state
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [step, setStep] = useState(1) // 1 = search, 2 = calculate

  // Step 1: Search for available tariffs
  const handleSearchTariffs = async () => {
    setLoading(true)
    setError("")

    try {
      const response = await fetch("http://localhost:8080/api/tariffs/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          reporter: selectedDestination,    // Backend expects 'reporter'
          partner: selectedSource,          // Backend expects 'partner'  
          tlCode: selectedProduct,          // Backend expects 'tlCode'
          year: 2023                        // Use fixed recent year
        }),
      })

      if (response.ok) {
        const data = await response.json()
        setAvailableTariffs(data.tariffs || [])
        setStep(2)
        setError(`✅ Found ${data.tariffs?.length || 0} tariff(s)`)
      } else {
        const errorData = await response.json()
        setError(`❌ ${errorData.error || 'Search failed'}`)
      }

    } catch (e) {
        const error = e as Error
        setError(`❌ Connection failed: ${error.message}`)
    }
    
    setLoading(false)
  }

  // Step 2: Calculate tariff with selected tariff
  const handleCalculate = async () => {
    setLoading(true)
    setError("")

    try {
      const response = await fetch("http://localhost:8080/api/tariffs/calculate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          reporterCode: selectedDestination,     // Add required reporterCode
          partnerCode: selectedSource,           // Add required partnerCode  
          productCode: selectedProduct,          // Add required productCode
          tariffId: parseInt(selectedTariff),    // Keep tariffId
          amountOfProduct: parseFloat(amountOfProduct), // Match backend field name
          currency: currency                     // Add currency selection
        }),
      })

      if (response.ok) {
        const data = await response.json()
        onCalculationResult(data.tariffAmount)
        setError(`✅ Tariff: $${data.tariffAmount} ${data.currency}`)
      } else {
        const errorData = await response.json()
        setError(`❌ ${errorData.error || 'Calculation failed'}`)
      }

    } catch (e) {
        const error = e as Error
        setError(`❌ Connection failed: ${error.message}`)
    }
    
    setLoading(false)
  }

  return (
    <div className="space-y-4">
      {step === 1 ? (
        // Step 1: Search Form
        <Card className="dark:bg-gray-800 dark:border-gray-700">
          <CardHeader className="pb-3">
            <CardTitle className="text-lg flex items-center gap-2 dark:text-white">
              <Search className="h-5 w-5" />
              Find Tariffs
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label htmlFor="product" className="dark:text-gray-300">Product Code</Label>
              <Select onValueChange={setSelectedProduct}>
                <SelectTrigger className="dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                  <SelectValue placeholder="Select product" />
                </SelectTrigger>
                <SelectContent className="dark:bg-gray-800 dark:border-gray-700">
                  <SelectItem value="76109099" className="dark:text-white dark:hover:bg-gray-700">76109099 - Aluminium Plates</SelectItem>
                  <SelectItem value="01021000" className="dark:text-white dark:hover:bg-gray-700">01021000 - Pure Bred Breeding Horses</SelectItem>
                  <SelectItem value="72084000" className="dark:text-white dark:hover:bg-gray-700">72084000 - Steel Products</SelectItem>
                  <SelectItem value="74130000" className="dark:text-white dark:hover:bg-gray-700">74130000 - Copper Wire</SelectItem>
                </SelectContent>
              </Select>
            </div>
            
            <div>
              <Label htmlFor="source" className="dark:text-gray-300">Source Country (Partner)</Label>
              <Select onValueChange={setSelectedSource}>
                <SelectTrigger className="dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                  <SelectValue placeholder="Select source" />
                </SelectTrigger>
                <SelectContent className="dark:bg-gray-800 dark:border-gray-700">
                  <SelectItem value="702" className="dark:text-white dark:hover:bg-gray-700">702 - Singapore</SelectItem>
                  <SelectItem value="840" className="dark:text-white dark:hover:bg-gray-700">840 - United States</SelectItem>
                  <SelectItem value="156" className="dark:text-white dark:hover:bg-gray-700">156 - China</SelectItem>
                  <SelectItem value="000" className="dark:text-white dark:hover:bg-gray-700">000 - World (Any Country)</SelectItem>
                </SelectContent>
              </Select>
            </div>
            
            <div>
              <Label htmlFor="destination" className="dark:text-gray-300">Destination Country (Reporter)</Label>
              <Select onValueChange={setSelectedDestination}>
                <SelectTrigger className="dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                  <SelectValue placeholder="Select destination" />
                </SelectTrigger>
                <SelectContent className="dark:bg-gray-800 dark:border-gray-700">
                  <SelectItem value="840" className="dark:text-white dark:hover:bg-gray-700">840 - United States</SelectItem>
                  <SelectItem value="918" className="dark:text-white dark:hover:bg-gray-700">918 - European Union</SelectItem>
                  <SelectItem value="392" className="dark:text-white dark:hover:bg-gray-700">392 - Japan</SelectItem>
                </SelectContent>
              </Select>
            </div>
            
            <Button 
              onClick={handleSearchTariffs} 
              disabled={loading || !selectedProduct || !selectedSource || !selectedDestination}
              className="w-full"
            >
              <Search className="h-4 w-4 mr-2" />
              {loading ? "Searching..." : "Search Available Tariffs"}
            </Button>
          </CardContent>
        </Card>
      ) : (
        // Step 2: Calculate Form
        <Card className="dark:bg-gray-800 dark:border-gray-700">
          <CardHeader className="pb-3">
            <CardTitle className="text-lg flex items-center gap-2 dark:text-white">
              <Calculator className="h-5 w-5" />
              Calculate Tariff
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <Button 
              variant="outline" 
              onClick={() => setStep(1)}
              className="w-full dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-700"
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Search
            </Button>
            
            <div>
              <Label className="dark:text-gray-300">Select Tariff</Label>
              <Select onValueChange={setSelectedTariff}>
                <SelectTrigger className="dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                  <SelectValue placeholder="Choose a tariff" />
                </SelectTrigger>
                <SelectContent className="dark:bg-gray-800 dark:border-gray-700">
                  {availableTariffs.map(tariff => (
                    <SelectItem 
                      key={tariff.tariffId} 
                      value={tariff.tariffId.toString()}
                      className="dark:text-white dark:hover:bg-gray-700"
                    >
                      {tariff.description || `Tariff ID: ${tariff.tariffId}`}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label className="dark:text-gray-300">Amount of Product</Label>
              <Input
                type="number"
                value={amountOfProduct}
                onChange={(e) => setAmountOfProduct(e.target.value)}
                placeholder="Enter quantity/amount (e.g., 1000)"
                step="0.01"
                min="0"
                className="dark:bg-gray-700 dark:border-gray-600 dark:text-white dark:placeholder-gray-400"
              />
            </div>

            <div>
              <Label className="dark:text-gray-300">Currency</Label>
              <Select onValueChange={setCurrency} value={currency}>
                <SelectTrigger className="dark:bg-gray-700 dark:border-gray-600 dark:text-white">
                  <SelectValue placeholder="Select currency" />
                </SelectTrigger>
                <SelectContent className="dark:bg-gray-800 dark:border-gray-700">
                  <SelectItem value="SGD" className="dark:text-white dark:hover:bg-gray-700">SGD - Singapore Dollar</SelectItem>
                  <SelectItem value="USD" className="dark:text-white dark:hover:bg-gray-700">USD - US Dollar</SelectItem>
                  <SelectItem value="EUR" className="dark:text-white dark:hover:bg-gray-700">EUR - Euro</SelectItem>
                  <SelectItem value="JPY" className="dark:text-white dark:hover:bg-gray-700">JPY - Japanese Yen</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <Button 
              onClick={handleCalculate} 
              disabled={loading || !selectedTariff || !amountOfProduct}
              className="w-full"
            >
              <Calculator className="h-4 w-4 mr-2" />
              {loading ? "Calculating..." : "Calculate Tariff"}
            </Button>
          </CardContent>
        </Card>
      )}

      {/* Status/Error Display */}
      {error && (
        <div className={`p-3 rounded-lg text-sm ${
          error.includes('✅') 
            ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' 
            : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
        }`}>
          {error}
        </div>
      )}
    </div>
  )
}