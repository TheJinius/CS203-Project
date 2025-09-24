"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface CalculateTabProps {
  onCalculationResult: (result: number | null) => void
}

export default function CalculateTab({ onCalculationResult }: CalculateTabProps) {
  // Search state
  const [selectedProduct, setSelectedProduct] = useState<string>("")
  const [selectedSource, setSelectedSource] = useState<string>("")
  const [selectedDestination, setSelectedDestination] = useState<string>("")
  const [selectedYear, setSelectedYear] = useState<string>("2023")
  
  // Results state
  const [availableTariffs, setAvailableTariffs] = useState<any[]>([])
  const [selectedTariff, setSelectedTariff] = useState<string>("")
  const [tradeValue, setTradeValue] = useState<string>("")
  
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
          reporterCode: selectedDestination,
          partnerCode: selectedSource,
          productCode: selectedProduct,
          year: parseInt(selectedYear)
        }),
      })

      if (response.ok) {
        const data = await response.json()
        setAvailableTariffs(data.tariffs)
        setStep(2)
        setError(`✅ Found ${data.count} tariff(s)`)
      } else {
        const errorData = await response.json()
        setError(`❌ ${errorData.error}`)
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
          tariffId: parseInt(selectedTariff),
          amountOfProduct: parseFloat(tradeValue),
          currency: "SGD"
        }),
      })

      if (response.ok) {
        const data = await response.json()
        onCalculationResult(data.tariffAmount)
        setError(`✅ Tariff: $${data.tariffAmount} ${data.currency}`)
      } else {
        const errorData = await response.json()
        setError(`❌ ${errorData.error}`)
      }

    } catch (e) {
        const error = e as Error
        setError(`❌ Connection failed: ${error.message}`)
    }
    
    setLoading(false)
  }

  return (
    <Card>
      <CardContent className="space-y-4">
        {step === 1 ? (
          // Step 1: Search Form
          <>
            <div>
              <Label htmlFor="product">Product</Label>
              <Select onValueChange={setSelectedProduct}>
                <SelectTrigger>
                  <SelectValue placeholder="Select product" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="76109099">Aluminium Plates</SelectItem>
                  <SelectItem value="1012100">Pure Bred Breeding Horses</SelectItem>
                  <SelectItem value="steel">Steel Products</SelectItem>
                  <SelectItem value="copper">Copper Wire</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label htmlFor="source">Source Country</Label>
              <Select onValueChange={setSelectedSource}>
                <SelectTrigger>
                  <SelectValue placeholder="Select source" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="702">Singapore</SelectItem>
                  <SelectItem value="840">United States</SelectItem>
                  <SelectItem value="156">China</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label htmlFor="destination">Destination Country</Label>
              <Select onValueChange={setSelectedDestination}>
                <SelectTrigger>
                  <SelectValue placeholder="Select destination" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="840">United States</SelectItem>
                  <SelectItem value="918">European Union</SelectItem>
                  <SelectItem value="392">Japan</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label htmlFor="year">Year</Label>
              <Select onValueChange={setSelectedYear} value={selectedYear}>
                <SelectTrigger>
                  <SelectValue placeholder="Select year" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="2023">2023</SelectItem>
                  <SelectItem value="2022">2022</SelectItem>
                  <SelectItem value="2021">2021</SelectItem>
                  <SelectItem value="2020">2020</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <Button onClick={handleSearchTariffs} disabled={loading}>
              {loading ? "Searching..." : "Search Available Tariffs"}
            </Button>
          </>
        ) : (
          // Step 2: Select tariff and calculate
          <>
            <Button variant="outline" onClick={() => setStep(1)}>
              ← Back to Search
            </Button>
            
            <div>
              <label className="block text-sm font-medium mb-2">Select Tariff</label>
              <Select onValueChange={setSelectedTariff}>
                <SelectTrigger>
                  <SelectValue placeholder="Choose a tariff" />
                </SelectTrigger>
                <SelectContent>
                  {availableTariffs.map(tariff => (
                    <SelectItem key={tariff.tariffId} value={tariff.tariffId.toString()}>
                      {tariff.description}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Trade Value</label>
              <Input
                type="number"
                value={tradeValue}
                onChange={(e) => setTradeValue(e.target.value)}
                placeholder="Enter trade value"
              />
            </div>

            <Button onClick={handleCalculate} disabled={loading || !selectedTariff}>
              {loading ? "Calculating..." : "Calculate Tariff"}
            </Button>
          </>
        )}

        {error && <p className="text-sm text-gray-600">{error}</p>}
      </CardContent>
    </Card>
  )
}