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
  const [selectedProduct, setSelectedProduct] = useState<string>("")
  const [selectedSource, setSelectedSource] = useState<string>("")
  const [selectedDestination, setSelectedDestination] = useState<string>("")
  const [tradeValue, setTradeValue] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<number | null>(null)

  const handleCalculate = async () => {
    if (!selectedProduct || !selectedSource || !selectedDestination || !tradeValue) {
      setError("Please select all fields and enter a trade value.")
      return
    }
    
    setLoading(true)
    setError(null)
    setResult(null)
    
    try {
      console.log("Attempting to connect to backend...")
      
      const response = await fetch("http://localhost:8080/api/tariffs/calculate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          reporterCode: selectedDestination,      // Changed from importerCountry
          partnerCode: selectedSource,            // Changed from exporterCountry
          productCode: selectedProduct,           // Changed from product
          amountOfProduct: tradeValue,
          currency: "SGD"
        }),
      })
      
      console.log("Response status:", response.status)
      
      const data = await response.json()
      console.log("Response data:", data)
      
      if (data.status === "success") {
        setResult(data.tariffAmount)
        onCalculationResult(data.tariffAmount)
        setError("✅ Backend connection successful!")
      } else {
        setError(`❌ Calculation failed: ${data.error || "Unknown error"}`)
      }
    } catch (e) {
      console.error("🔥 Network error:", e)
      setError(`❌ Cannot connect to backend: ${e}`)
    }
    
    setLoading(false)
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">Calculate Tariff</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div>
          <Label htmlFor="product">Product</Label>
          <Select onValueChange={setSelectedProduct}>
            <SelectTrigger>
              <SelectValue placeholder="Select product" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="76109099">Aluminium Plates</SelectItem>
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
          <Label htmlFor="value">Trade Value ($)</Label>
          <Input
            id="value"
            type="number"
            placeholder="Enter trade value"
            onChange={(e) => setTradeValue(Number.parseFloat(e.target.value))}
          />
        </div>
        <Button className="w-full" onClick={handleCalculate} disabled={loading}>
          {loading ? "Testing Backend Connection..." : "Calculate Tariff"}
        </Button>
        
        {/* Connection Status */}
        {error && (
          <div className={`text-sm p-2 rounded ${
            error.includes("✅") ? "text-green-600 bg-green-50" : "text-red-600 bg-red-50"
          }`}>
            {error}
          </div>
        )}
        
        {result !== null && (
          <Card className="bg-accent/10">
            <CardContent className="pt-4">
              <div className="text-center">
                <p className="text-sm text-muted-foreground">Estimated Tariff</p>
                <p className="text-2xl font-bold text-accent">${result.toFixed(2)}</p>
                <p className="text-xs text-green-600 mt-1">✅ Backend Connected</p>
              </div>
            </CardContent>
          </Card>
        )}
        
        {/* Instructions */}
        <div className="text-xs text-muted-foreground p-2 bg-muted rounded">
          💡 Fill out all fields and click Calculate to test backend connection
        </div>
      </CardContent>
    </Card>
  )
}