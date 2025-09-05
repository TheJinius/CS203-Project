"use client"

import { useState } from "react"
import { Menu, X, Globe, Calculator, Package, MapPin, FileText, TrendingUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"

export default function TariffCalculatorPage() {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [activeTab, setActiveTab] = useState("calculate")
  const [calculationResult, setCalculationResult] = useState<number | null>(null)

  const handleCalculate = (value: number) => {
    // Mock calculation - in real app this would call the API
    const mockTariffRate = 0.15 // 15%
    const result = value * mockTariffRate
    setCalculationResult(result)
  }

  const sidebarItems = [
    { id: "calculate", label: "Calculate Tariff", icon: Calculator },
    { id: "products", label: "Products", icon: Package },
    { id: "countries", label: "Countries", icon: MapPin },
    { id: "tariffs", label: "Tariffs", icon: FileText },
    { id: "results", label: "Results", icon: TrendingUp },
  ]

  return (
    <div className="flex h-screen bg-background">
      {/* Sidebar */}
      <div
        className={`${sidebarOpen ? "w-80" : "w-0"} transition-all duration-300 overflow-hidden bg-sidebar border-r border-sidebar-border`}
      >
        <div className="p-4">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2">
              <Globe className="h-6 w-6 text-sidebar-accent" />
              <h1 className="text-lg font-semibold text-sidebar-foreground">Tariff Calculator</h1>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setSidebarOpen(false)}
              className="text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Navigation */}
          <nav className="space-y-2 mb-6">
            {sidebarItems.map((item) => {
              const Icon = item.icon
              return (
                <Button
                  key={item.id}
                  variant={activeTab === item.id ? "default" : "ghost"}
                  className={`w-full justify-start gap-2 ${
                    activeTab === item.id
                      ? "bg-sidebar-primary text-sidebar-primary-foreground"
                      : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                  }`}
                  onClick={() => setActiveTab(item.id)}
                >
                  <Icon className="h-4 w-4" />
                  {item.label}
                </Button>
              )
            })}
          </nav>

          <Separator className="mb-6" />

          {/* Content based on active tab */}
          <div className="space-y-4">
            {activeTab === "calculate" && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-sm">Calculate Tariff</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="product">Product</Label>
                    <Select>
                      <SelectTrigger>
                        <SelectValue placeholder="Select product" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="aluminium">Aluminium Plates</SelectItem>
                        <SelectItem value="steel">Steel Products</SelectItem>
                        <SelectItem value="copper">Copper Wire</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="source">Source Country</Label>
                    <Select>
                      <SelectTrigger>
                        <SelectValue placeholder="Select source" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="sg">Singapore</SelectItem>
                        <SelectItem value="us">United States</SelectItem>
                        <SelectItem value="cn">China</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="destination">Destination Country</Label>
                    <Select>
                      <SelectTrigger>
                        <SelectValue placeholder="Select destination" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="us">United States</SelectItem>
                        <SelectItem value="eu">European Union</SelectItem>
                        <SelectItem value="jp">Japan</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="value">Trade Value ($)</Label>
                    <Input
                      id="value"
                      type="number"
                      placeholder="Enter trade value"
                      onChange={(e) => {
                        const value = Number.parseFloat(e.target.value)
                        if (value > 0) handleCalculate(value)
                      }}
                    />
                  </div>
                  {calculationResult !== null && (
                    <Card className="bg-accent/10">
                      <CardContent className="pt-4">
                        <div className="text-center">
                          <p className="text-sm text-muted-foreground">Estimated Tariff</p>
                          <p className="text-2xl font-bold text-accent">${calculationResult.toFixed(2)}</p>
                        </div>
                      </CardContent>
                    </Card>
                  )}
                </CardContent>
              </Card>
            )}

            {activeTab === "products" && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-sm">Product Management</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="hs-code">HS Code</Label>
                    <Input id="hs-code" placeholder="e.g., VA123" />
                  </div>
                  <div>
                    <Label htmlFor="product-name">Product Name</Label>
                    <Input id="product-name" placeholder="e.g., Aluminium plates" />
                  </div>
                  <div>
                    <Label htmlFor="category">Category</Label>
                    <Select>
                      <SelectTrigger>
                        <SelectValue placeholder="Select category" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="metals">Metals</SelectItem>
                        <SelectItem value="textiles">Textiles</SelectItem>
                        <SelectItem value="electronics">Electronics</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <Button className="w-full">Add Product</Button>
                </CardContent>
              </Card>
            )}

            {activeTab === "countries" && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-sm">Country Management</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="iso-code">ISO Code</Label>
                    <Input id="iso-code" placeholder="e.g., SG" />
                  </div>
                  <div>
                    <Label htmlFor="country-name">Country Name</Label>
                    <Input id="country-name" placeholder="e.g., Singapore" />
                  </div>
                  <Button className="w-full">Add Country</Button>
                </CardContent>
              </Card>
            )}

            {activeTab === "tariffs" && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-sm">Tariff Management</CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="tariff-name">Tariff Name</Label>
                    <Input id="tariff-name" placeholder="e.g., US Tariff 2025" />
                  </div>
                  <div>
                    <Label htmlFor="tariff-code">Tariff Code</Label>
                    <Input id="tariff-code" placeholder="e.g., TAR2201" />
                  </div>
                  <div>
                    <Label htmlFor="tariff-type">Tariff Type</Label>
                    <Select>
                      <SelectTrigger>
                        <SelectValue placeholder="Select type" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ad-valorem">Ad Valorem</SelectItem>
                        <SelectItem value="compound">Compound</SelectItem>
                        <SelectItem value="reciprocal">Reciprocal</SelectItem>
                        <SelectItem value="specific">Specific</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <Label htmlFor="rate-value">Rate Value</Label>
                    <Input id="rate-value" type="number" step="0.01" placeholder="e.g., 0.15" />
                  </div>
                  <Button className="w-full">Add Tariff</Button>
                </CardContent>
              </Card>
            )}

            {activeTab === "results" && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-sm">Calculation Results</CardTitle>
                </CardHeader>
                <CardContent>
                  {calculationResult !== null ? (
                    <div className="space-y-2">
                      <div className="flex justify-between">
                        <span className="text-sm text-muted-foreground">Tariff Amount:</span>
                        <span className="font-medium">${calculationResult.toFixed(2)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-sm text-muted-foreground">Rate Applied:</span>
                        <span className="font-medium">15%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-sm text-muted-foreground">Status:</span>
                        <span className="text-green-600 font-medium">Active</span>
                      </div>
                    </div>
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      No calculations yet. Use the Calculate tab to get started.
                    </p>
                  )}
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <header className="h-16 border-b border-border bg-card flex items-center px-4">
          {!sidebarOpen && (
            <Button variant="ghost" size="sm" onClick={() => setSidebarOpen(true)} className="mr-4">
              <Menu className="h-4 w-4" />
            </Button>
          )}
          <h2 className="text-lg font-semibold text-card-foreground">Global Trade Map</h2>
        </header>

        {/* Map Area */}
        <main className="flex-1 bg-gradient-to-br from-blue-50 to-blue-100 relative overflow-hidden">
          {/* Placeholder world map */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center space-y-4">
              <Globe className="h-24 w-24 text-blue-300 mx-auto" />
              <div>
                <h3 className="text-xl font-semibold text-gray-700 mb-2">Interactive World Map</h3>
                <p className="text-gray-500 max-w-md">
                  This area will display an interactive world map showing trade routes, tariff zones, and calculation
                  results. Click on countries to view tariff information and trade relationships.
                </p>
              </div>
            </div>
          </div>

          {/* Map overlay info */}
          <div className="absolute top-4 right-4 space-y-2">
            <Card className="w-64">
              <CardContent className="p-4">
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                  <span className="text-sm">Active Trade Routes</span>
                </div>
                <div className="flex items-center gap-2 mb-2">
                  <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                  <span className="text-sm">Pending Tariffs</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                  <span className="text-sm">High Tariff Zones</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </main>
      </div>
    </div>
  )
}
