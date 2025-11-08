"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Calendar, MapPin, Package, TrendingUp, Hash, GripVertical, X, Route } from "lucide-react"

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

export interface CombinedRoute {
  id: string
  name: string
  legs: CalculationHistory[]
  totalCost: number
  currency: string
  createdAt: Date
}

interface ResultsTabProps {
  calculationResult: number | null
  calculationHistory: CalculationHistory[]
  currency: string
}

export default function ResultsTab({ calculationResult, calculationHistory, currency }: ResultsTabProps) {
  const [combinedRoutes, setCombinedRoutes] = useState<CombinedRoute[]>([])
  const [draggedItem, setDraggedItem] = useState<CalculationHistory | null>(null)
  const [dropTarget, setDropTarget] = useState<string | null>(null)
  const [builderLegs, setBuilderLegs] = useState<CalculationHistory[]>([])

  // Load combined routes from localStorage
  useEffect(() => {
    const stored = localStorage.getItem('combinedRoutes')
    if (stored) {
      try {
        const parsed = JSON.parse(stored)
        setCombinedRoutes(parsed)
      } catch (e) {
        console.error('Failed to load combined routes', e)
      }
    }
  }, [])

  // Save combined routes to localStorage whenever they change
  useEffect(() => {
    if (combinedRoutes.length > 0) {
      localStorage.setItem('combinedRoutes', JSON.stringify(combinedRoutes))
    }
  }, [combinedRoutes])

  const handleDragStart = (calc: CalculationHistory) => {
    setDraggedItem(calc)
  }

  const handleDragEnd = () => {
    setDraggedItem(null)
    setDropTarget(null)
  }

  const handleDragOver = (e: React.DragEvent, targetId: string) => {
    e.preventDefault()
    setDropTarget(targetId)
  }

  const handleDragLeave = () => {
    setDropTarget(null)
  }

  const handleDropOnBuilder = (e: React.DragEvent) => {
    e.preventDefault()
    if (draggedItem) {
      // Check if the leg connects properly (destination of last leg matches source of new leg)
      if (builderLegs.length > 0) {
        const lastLeg = builderLegs[builderLegs.length - 1]
        if (lastLeg.destinationCountry !== draggedItem.sourceCountry) {
          alert(`Cannot add this leg: Last destination was ${lastLeg.destinationCountry}, but this leg starts from ${draggedItem.sourceCountry}`)
          setDropTarget(null)
          return
        }
      }
      setBuilderLegs([...builderLegs, draggedItem])
    }
    setDropTarget(null)
  }

  const removeLegFromBuilder = (legId: string) => {
    setBuilderLegs(builderLegs.filter(leg => leg.id !== legId))
  }

  const saveRoute = () => {
    if (builderLegs.length < 2) {
      alert("A combined route must have at least 2 legs")
      return
    }

    // Calculate total cost (convert all to the same currency if needed)
    const totalCost = builderLegs.reduce((sum, leg) => sum + leg.tariffAmount, 0)
    
    // Generate route name
    const countries = [builderLegs[0].sourceCountry, ...builderLegs.map(leg => leg.destinationCountry)]
    const routeName = countries.join(" → ")

    const newRoute: CombinedRoute = {
      id: `route-${Date.now()}`,
      name: routeName,
      legs: [...builderLegs],
      totalCost,
      currency: builderLegs[0].currency,
      createdAt: new Date()
    }

    setCombinedRoutes([newRoute, ...combinedRoutes])
    setBuilderLegs([])
  }

  const deleteRoute = (routeId: string) => {
    setCombinedRoutes(combinedRoutes.filter(route => route.id !== routeId))
  }

  return (
    <div className="space-y-4">
      {/* Current Result Card */}
      {calculationResult !== null && (
        <Card className="bg-blue-50 dark:bg-blue-900/10 border-blue-200 dark:border-blue-800">
          <CardHeader className="pb-3">
            <CardTitle className="text-base flex items-center gap-2 text-blue-900 dark:text-blue-100">
              <TrendingUp className="h-5 w-5" />
              Current Calculation
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-blue-600 dark:text-blue-400">
              {currency} {calculationResult.toFixed(2)}
            </div>
            <p className="text-sm text-blue-700 dark:text-blue-300 mt-2">
              Latest tariff calculation result
            </p>
          </CardContent>
        </Card>
      )}

      {/* Route Builder */}
      <Card className="bg-amber-50 dark:bg-amber-900/10 border-amber-200 dark:border-amber-800">
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2 text-amber-900 dark:text-amber-100">
            <Route className="h-5 w-5" />
            Route Builder
          </CardTitle>
          <p className="text-xs text-amber-700 dark:text-amber-300 mt-1">
            Drag calculations here to combine them into multi-leg routes
          </p>
        </CardHeader>
        <CardContent>
          <div
            onDrop={handleDropOnBuilder}
            onDragOver={(e) => handleDragOver(e, 'builder')}
            onDragLeave={handleDragLeave}
            className={`min-h-[120px] border-2 border-dashed rounded-lg p-3 transition-colors ${
              dropTarget === 'builder'
                ? 'border-amber-500 bg-amber-100 dark:bg-amber-900/20'
                : 'border-amber-300 dark:border-amber-700 bg-white dark:bg-slate-800'
            }`}
          >
            {builderLegs.length === 0 ? (
              <div className="text-center py-6 text-amber-600 dark:text-amber-400 text-sm">
                Drop calculations here to build a route
              </div>
            ) : (
              <div className="space-y-2">
                {builderLegs.map((leg, index) => (
                  <div key={leg.id} className="bg-white dark:bg-slate-700 border border-amber-200 dark:border-amber-800 rounded p-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 flex-1">
                        <Badge variant="outline" className="text-xs">Leg {index + 1}</Badge>
                        <div className="text-sm">
                          <span className="font-medium">{leg.sourceCountry}</span>
                          <span className="mx-1 text-slate-400">→</span>
                          <span className="font-medium">{leg.destinationCountry}</span>
                        </div>
                        <div className="text-xs text-slate-600 dark:text-slate-400">
                          {leg.productCode}
                        </div>
                        <div className="text-sm font-semibold text-green-600 dark:text-green-400 ml-auto">
                          {leg.currency} {leg.tariffAmount.toFixed(2)}
                        </div>
                      </div>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => removeLegFromBuilder(leg.id)}
                        className="h-6 w-6 p-0 text-red-600 hover:text-red-700 hover:bg-red-100 dark:hover:bg-red-900/20"
                      >
                        <X className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>
                ))}
                <div className="flex items-center justify-between pt-2 border-t border-amber-200 dark:border-amber-700">
                  <div className="text-sm font-semibold text-amber-900 dark:text-amber-100">
                    Total Cost: {builderLegs[0]?.currency} {builderLegs.reduce((sum, leg) => sum + leg.tariffAmount, 0).toFixed(2)}
                  </div>
                  <Button
                    onClick={saveRoute}
                    size="sm"
                    className="bg-amber-600 hover:bg-amber-700 text-white"
                  >
                    Save Route
                  </Button>
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Combined Routes */}
      {combinedRoutes.length > 0 && (
        <Card className="bg-green-50 dark:bg-green-900/10 border-green-200 dark:border-green-800">
          <CardHeader className="pb-3">
            <CardTitle className="text-base flex items-center gap-2 text-green-900 dark:text-green-100">
              <Route className="h-5 w-5" />
              Saved Multi-Leg Routes ({combinedRoutes.length})
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3 max-h-[400px] overflow-y-auto">
              {combinedRoutes.map((route) => (
                <Card key={route.id} className="bg-white dark:bg-slate-800 border-green-200 dark:border-green-800">
                  <CardContent className="p-4 space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="font-semibold text-green-900 dark:text-green-100 text-sm mb-1">
                          {route.name}
                        </div>
                        <div className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-400">
                          <Calendar className="h-3 w-3" />
                          {new Date(route.createdAt).toLocaleString()}
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-xl font-bold text-green-600 dark:text-green-400">
                          {route.currency} {route.totalCost.toFixed(2)}
                        </div>
                        <Badge variant="outline" className="text-xs mt-1">
                          {route.legs.length} legs
                        </Badge>
                      </div>
                    </div>

                    {/* Route Legs */}
                    <div className="space-y-2">
                      {route.legs.map((leg, index) => (
                        <div key={leg.id} className="bg-slate-50 dark:bg-slate-700 rounded p-2 text-xs">
                          <div className="flex items-center justify-between mb-1">
                            <Badge variant="outline" className="text-xs">Leg {index + 1}</Badge>
                            <div className="font-semibold text-green-600 dark:text-green-400">
                              {leg.currency} {leg.tariffAmount.toFixed(2)}
                            </div>
                          </div>
                          <div className="flex items-center gap-2 text-slate-700 dark:text-slate-300">
                            <MapPin className="h-3 w-3 text-blue-600 dark:text-blue-400" />
                            <span className="font-medium">{leg.sourceCountry}</span>
                            <span className="text-slate-400">→</span>
                            <span className="font-medium">{leg.destinationCountry}</span>
                          </div>
                          <div className="flex items-center gap-2 mt-1 text-slate-600 dark:text-slate-400">
                            <Package className="h-3 w-3" />
                            <span>{leg.productCode} - {leg.productDescription}</span>
                          </div>
                        </div>
                      ))}
                    </div>

                    <Button
                      variant="destructive"
                      size="sm"
                      onClick={() => deleteRoute(route.id)}
                      className="w-full h-7 text-xs"
                    >
                      <X className="h-3 w-3 mr-1" />
                      Delete Route
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Calculation History */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base text-slate-900 dark:text-slate-100">
            Individual Calculations ({calculationHistory.length})
          </CardTitle>
          <p className="text-xs text-slate-600 dark:text-slate-400 mt-1">
            Drag and drop to combine into routes
          </p>
        </CardHeader>
        <CardContent>
          {calculationHistory.length > 0 ? (
            <div className="space-y-3 max-h-[600px] overflow-y-auto">
              {calculationHistory.map((calc) => (
                <Card
                  key={calc.id}
                  draggable
                  onDragStart={() => handleDragStart(calc)}
                  onDragEnd={handleDragEnd}
                  className={`bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 cursor-grab active:cursor-grabbing transition-shadow hover:shadow-md ${
                    draggedItem?.id === calc.id ? 'opacity-50' : ''
                  }`}
                >
                  <CardContent className="p-4 space-y-3">
                    {/* Drag Handle */}
                    <div className="flex items-start gap-2">
                      <GripVertical className="h-5 w-5 text-slate-400 dark:text-slate-500 flex-shrink-0 mt-1" />
                      <div className="flex-1 space-y-3">
                        {/* Header with Timestamp and Amount */}
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-400">
                            <Calendar className="h-3.5 w-3.5" />
                            {new Date(calc.timestamp).toLocaleString()}
                          </div>
                          <div className="text-right">
                            <div className="text-lg font-bold text-green-600 dark:text-green-400">
                              {calc.currency} {calc.tariffAmount.toFixed(2)}
                            </div>
                          </div>
                        </div>

                        {/* Route Information */}
                        <div className="flex items-center gap-2 text-sm">
                          <MapPin className="h-4 w-4 text-blue-600 dark:text-blue-400 flex-shrink-0" />
                          <span className="text-slate-700 dark:text-slate-300">
                            <span className="font-medium">{calc.sourceCountry}</span>
                            <span className="mx-2 text-slate-400">→</span>
                            <span className="font-medium">{calc.destinationCountry}</span>
                          </span>
                        </div>

                        {/* Product Information */}
                        <div className="space-y-1">
                          <div className="flex items-start gap-2 text-sm">
                            <Package className="h-4 w-4 text-purple-600 dark:text-purple-400 flex-shrink-0 mt-0.5" />
                            <div className="flex-1 min-w-0">
                              <div className="font-medium text-slate-900 dark:text-slate-100">
                                HS Code: {calc.productCode}
                              </div>
                              <div className="text-xs text-slate-600 dark:text-slate-400 line-clamp-2">
                                {calc.productDescription}
                              </div>
                            </div>
                          </div>
                        </div>

                        {/* Additional Details */}
                        <div className="flex flex-wrap gap-2 pt-2 border-t border-slate-200 dark:border-slate-700">
                          <Badge variant="outline" className="text-xs bg-white dark:bg-slate-700">
                            <Hash className="h-3 w-3 mr-1" />
                            Tariff ID: {calc.tariffId}
                          </Badge>
                          {calc.dutyType && (
                            <Badge variant="outline" className="text-xs bg-white dark:bg-slate-700">
                              {calc.dutyType}
                            </Badge>
                          )}
                          {calc.rate !== undefined && (
                            <Badge variant="outline" className="text-xs bg-white dark:bg-slate-700">
                              Rate: {calc.rate}%
                            </Badge>
                          )}
                          <Badge variant="outline" className="text-xs bg-white dark:bg-slate-700">
                            Year: {calc.year}
                          </Badge>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <TrendingUp className="h-12 w-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
              <p className="text-sm text-slate-600 dark:text-slate-400">
                No calculations yet. Use the Calculate tab to get started.
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}