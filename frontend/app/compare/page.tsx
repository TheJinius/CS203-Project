"use client"

import React, { useState, useEffect, useMemo } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import WorldMap, { type OptimalRoutesData } from "@/components/WorldMap"
import TopBar from "@/components/TopBar"
import { ProtectedRoute } from "@/components/ProtectedRoute"
import { ArrowLeft, Route, MapPin, DollarSign, Clock, Leaf, AlertTriangle, GripVertical, X, BarChart3, TrendingUp, Award, Map } from "lucide-react"
import { useRouter } from "next/navigation"

interface CalculationHistory {
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

interface CombinedRoute {
  id: string
  name: string
  legs: CalculationHistory[]
  totalCost: number
  currency: string
  createdAt: Date
}

interface RouteMetrics {
  distance_km?: number
  cost_usd?: number
  time_hours?: number
  co2_kg?: number
  risk_score?: number
  transport_type?: string
}

interface ComparisonRoute {
  id: string
  name: string
  tariffCost: number
  currency: string
  legs: CalculationHistory[]
  type: 'single' | 'combined'
  routeMetrics?: RouteMetrics
}

type MetricType = 'cost' | 'time' | 'carbon' | 'risk'

export default function ComparePage() {
  const router = useRouter()
  const [calculations, setCalculations] = useState<CalculationHistory[]>([])
  const [combinedRoutes, setCombinedRoutes] = useState<CombinedRoute[]>([])
  const [selectedForComparison, setSelectedForComparison] = useState<ComparisonRoute[]>([])
  const [draggedItem, setDraggedItem] = useState<ComparisonRoute | null>(null)
  const [dropTarget, setDropTarget] = useState<boolean>(false)
  const [optimalRoutesData, setOptimalRoutesData] = useState<OptimalRoutesData | null>(null)
  const [selectedMetric, setSelectedMetric] = useState<MetricType>('cost')
  const [showMap, setShowMap] = useState<boolean>(false)

  // Load data from localStorage on mount
  useEffect(() => {
    const loadData = () => {
      // In a real app, this would come from a context or API
      // For now, we'll simulate getting data from localStorage
      const storedCalcs = localStorage.getItem('calculationHistory')
      const storedRoutes = localStorage.getItem('combinedRoutes')
      
      if (storedCalcs) {
        try {
          const parsed = JSON.parse(storedCalcs)
          setCalculations(parsed)
        } catch (e) {
          console.error('Failed to parse calculations', e)
        }
      }
      
      if (storedRoutes) {
        try {
          const parsed = JSON.parse(storedRoutes)
          setCombinedRoutes(parsed)
        } catch (e) {
          console.error('Failed to parse routes', e)
        }
      }
    }

    loadData()
  }, [])

  const handleDragStart = (item: ComparisonRoute) => {
    setDraggedItem(item)
  }

  const handleDragEnd = () => {
    setDraggedItem(null)
    setDropTarget(false)
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setDropTarget(true)
  }

  const handleDragLeave = () => {
    setDropTarget(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    if (draggedItem && !selectedForComparison.find(r => r.id === draggedItem.id)) {
      setSelectedForComparison([...selectedForComparison, draggedItem])
      // TODO: Fetch optimal routes for this calculation
    }
    setDropTarget(false)
  }

  const removeFromComparison = (id: string) => {
    setSelectedForComparison(selectedForComparison.filter(r => r.id !== id))
  }

  const clearComparison = () => {
    setSelectedForComparison([])
    setOptimalRoutesData(null)
  }

  // Convert calculations to comparison format
  const allRoutes: ComparisonRoute[] = [
    ...calculations.map(calc => ({
      id: calc.id,
      name: `${calc.sourceCountry} → ${calc.destinationCountry}`,
      tariffCost: calc.tariffAmount,
      currency: calc.currency,
      legs: [calc],
      type: 'single' as const
    })),
    ...combinedRoutes.map(route => ({
      id: route.id,
      name: route.name,
      tariffCost: route.totalCost,
      currency: route.currency,
      legs: route.legs,
      type: 'combined' as const
    }))
  ]

  // Calculate totals for comparison
  const totalTariffCost = selectedForComparison.reduce((sum, route) => sum + route.tariffCost, 0)
  const averageTariffCost = selectedForComparison.length > 0 ? totalTariffCost / selectedForComparison.length : 0

  // Calculate best route based on selected metric
  const bestRoute = useMemo(() => {
    if (selectedForComparison.length === 0) return null

    let best = selectedForComparison[0]
    
    switch (selectedMetric) {
      case 'cost':
        best = selectedForComparison.reduce((min, route) => 
          route.tariffCost < min.tariffCost ? route : min
        , selectedForComparison[0])
        break
      case 'time':
        // Simulated time based on number of legs (more legs = more time)
        best = selectedForComparison.reduce((min, route) => 
          route.legs.length < min.legs.length ? route : min
        , selectedForComparison[0])
        break
      case 'carbon':
        // Simulated carbon based on legs and distance (simplified)
        best = selectedForComparison.reduce((min, route) => 
          route.legs.length < min.legs.length ? route : min
        , selectedForComparison[0])
        break
      case 'risk':
        // Lower risk for direct routes
        best = selectedForComparison.reduce((min, route) => 
          route.legs.length < min.legs.length ? route : min
        , selectedForComparison[0])
        break
    }
    
    return best
  }, [selectedForComparison, selectedMetric])

  // Get metric value for display
  const getMetricValue = (route: ComparisonRoute, metric: MetricType): number => {
    switch (metric) {
      case 'cost':
        return route.tariffCost
      case 'time':
        // Simulated: 24h per leg
        return route.legs.length * 24
      case 'carbon':
        // Simulated: 100kg CO2 per leg
        return route.legs.length * 100
      case 'risk':
        // Simulated: risk score 1-10, more legs = higher risk
        return Math.min(route.legs.length * 2, 10)
      default:
        return 0
    }
  }

  const getMetricUnit = (metric: MetricType): string => {
    switch (metric) {
      case 'cost': return selectedForComparison[0]?.currency || 'USD'
      case 'time': return 'hours'
      case 'carbon': return 'kg CO₂'
      case 'risk': return '/10'
      default: return ''
    }
  }

  const getMetricIcon = (metric: MetricType) => {
    switch (metric) {
      case 'cost': return DollarSign
      case 'time': return Clock
      case 'carbon': return Leaf
      case 'risk': return AlertTriangle
      default: return BarChart3
    }
  }

  const getMetricColor = (metric: MetricType) => {
    switch (metric) {
      case 'cost': return 'text-green-600 dark:text-green-400'
      case 'time': return 'text-blue-600 dark:text-blue-400'
      case 'carbon': return 'text-emerald-600 dark:text-emerald-400'
      case 'risk': return 'text-red-600 dark:text-red-400'
      default: return 'text-slate-600 dark:text-slate-400'
    }
  }

  const getMetricBgColor = (metric: MetricType) => {
    switch (metric) {
      case 'cost': return 'bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800'
      case 'time': return 'bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800'
      case 'carbon': return 'bg-emerald-50 dark:bg-emerald-900/20 border-emerald-200 dark:border-emerald-800'
      case 'risk': return 'bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800'
      default: return 'bg-slate-50 dark:bg-slate-900/20'
    }
  }

  return (
    <ProtectedRoute>
      <div className="flex h-screen bg-slate-100 dark:bg-slate-950 text-slate-900 dark:text-slate-100 overflow-hidden">
        {/* Left Panel - Route Selection */}
        <div className="w-96 flex-shrink-0 bg-white dark:bg-slate-800 border-r border-slate-200 dark:border-slate-700 flex flex-col">
          <div className="p-4 border-b border-slate-200 dark:border-slate-700">
            <div className="flex items-center gap-3 mb-4">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => router.push('/')}
                className="text-slate-600 dark:text-slate-300"
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back
              </Button>
            </div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100 mb-2">
              Route Comparison
            </h1>
            <p className="text-sm text-slate-600 dark:text-slate-400">
              Drag routes to the comparison area to analyze and visualize them on the map
            </p>
          </div>

          {/* Available Routes */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {/* Single Calculations */}
            {calculations.length > 0 && (
              <div>
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2">
                  Single Routes ({calculations.length})
                </h3>
                <div className="space-y-2">
                  {allRoutes.filter(r => r.type === 'single').map((route) => (
                    <Card
                      key={route.id}
                      draggable
                      onDragStart={() => handleDragStart(route)}
                      onDragEnd={handleDragEnd}
                      className={`cursor-grab active:cursor-grabbing transition-all hover:shadow-md ${
                        draggedItem?.id === route.id ? 'opacity-50' : ''
                      } ${
                        selectedForComparison.find(r => r.id === route.id) ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20' : ''
                      }`}
                    >
                      <CardContent className="p-3">
                        <div className="flex items-start gap-2">
                          <GripVertical className="h-4 w-4 text-slate-400 flex-shrink-0 mt-1" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 mb-1">
                              <MapPin className="h-3 w-3 text-blue-600 dark:text-blue-400" />
                              <span className="text-sm font-medium truncate">{route.name}</span>
                            </div>
                            <div className="text-xs text-slate-600 dark:text-slate-400 truncate">
                              {route.legs[0].productCode} - {route.legs[0].productDescription}
                            </div>
                            <div className="text-sm font-bold text-green-600 dark:text-green-400 mt-1">
                              {route.currency} {route.tariffCost.toFixed(2)}
                            </div>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            )}

            {/* Combined Routes */}
            {combinedRoutes.length > 0 && (
              <div>
                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2">
                  Multi-Leg Routes ({combinedRoutes.length})
                </h3>
                <div className="space-y-2">
                  {allRoutes.filter(r => r.type === 'combined').map((route) => (
                    <Card
                      key={route.id}
                      draggable
                      onDragStart={() => handleDragStart(route)}
                      onDragEnd={handleDragEnd}
                      className={`cursor-grab active:cursor-grabbing transition-all hover:shadow-md ${
                        draggedItem?.id === route.id ? 'opacity-50' : ''
                      } ${
                        selectedForComparison.find(r => r.id === route.id) ? 'border-green-500 bg-green-50 dark:bg-green-900/20' : ''
                      }`}
                    >
                      <CardContent className="p-3">
                        <div className="flex items-start gap-2">
                          <GripVertical className="h-4 w-4 text-slate-400 flex-shrink-0 mt-1" />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 mb-1">
                              <Route className="h-3 w-3 text-green-600 dark:text-green-400" />
                              <span className="text-sm font-medium">{route.name}</span>
                            </div>
                            <Badge variant="outline" className="text-xs mb-1">
                              {route.legs.length} legs
                            </Badge>
                            <div className="text-sm font-bold text-green-600 dark:text-green-400 mt-1">
                              {route.currency} {route.tariffCost.toFixed(2)}
                            </div>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  ))}
                </div>
              </div>
            )}

            {allRoutes.length === 0 && (
              <div className="text-center py-12">
                <Route className="h-12 w-12 text-slate-300 dark:text-slate-600 mx-auto mb-3" />
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  No routes available. Calculate some tariffs first.
                </p>
                <Button
                  onClick={() => router.push('/')}
                  size="sm"
                  className="mt-4"
                >
                  Go to Calculator
                </Button>
              </div>
            )}
          </div>
        </div>

        {/* Right Panel - Comparison & Map */}
        <div className="flex-1 flex flex-col min-w-0">
          <TopBar sidebarOpen={false} onToggleSidebar={() => {}} />

          <div className="flex-1 flex flex-col overflow-hidden">
            {/* Comparison Drop Zone */}
            <div className="p-4 bg-slate-50 dark:bg-slate-900 border-b border-slate-200 dark:border-slate-700">
              <div
                onDrop={handleDrop}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                className={`min-h-[120px] border-2 border-dashed rounded-lg p-4 transition-all ${
                  dropTarget
                    ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                    : 'border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800'
                }`}
              >
                {selectedForComparison.length === 0 ? (
                  <div className="text-center py-6">
                    <Route className="h-8 w-8 text-slate-400 dark:text-slate-500 mx-auto mb-2" />
                    <p className="text-sm text-slate-600 dark:text-slate-400">
                      Drop routes here to compare
                    </p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="font-semibold text-slate-900 dark:text-slate-100">
                        Comparing {selectedForComparison.length} route{selectedForComparison.length !== 1 ? 's' : ''}
                      </h3>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={clearComparison}
                        className="text-red-600 hover:text-red-700 hover:bg-red-100 dark:hover:bg-red-900/20"
                      >
                        <X className="h-4 w-4 mr-1" />
                        Clear All
                      </Button>
                    </div>

                    {/* Metric Selector */}
                    <div className="flex gap-2 pb-3 border-b border-slate-200 dark:border-slate-600">
                      <span className="text-sm font-medium text-slate-700 dark:text-slate-300 mr-2">Optimize for:</span>
                      {(['cost', 'time', 'carbon', 'risk'] as MetricType[]).map((metric) => {
                        const Icon = getMetricIcon(metric)
                        const isSelected = selectedMetric === metric
                        return (
                          <Button
                            key={metric}
                            variant={isSelected ? "default" : "outline"}
                            size="sm"
                            onClick={() => setSelectedMetric(metric)}
                            className={`gap-2 ${isSelected ? getMetricBgColor(metric) + ' border-0' : ''}`}
                          >
                            <Icon className={`h-3.5 w-3.5 ${isSelected ? getMetricColor(metric) : ''}`} />
                            <span className="capitalize">{metric}</span>
                          </Button>
                        )
                      })}
                    </div>

                    {/* Map Toggle Button */}
                    <div className="pb-3 border-b border-slate-200 dark:border-slate-600">
                      <Button
                        onClick={() => setShowMap(!showMap)}
                        variant={showMap ? "default" : "outline"}
                        className="w-full gap-2"
                      >
                        <Map className="h-4 w-4" />
                        {showMap ? 'Hide Map View' : 'Show Map View'}
                      </Button>
                    </div>

                    {/* Analytics View */}
                    {!showMap && (
                      <>
                        {/* Routes Grid */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2">
                          {selectedForComparison.map((route) => {
                            const isBest = bestRoute?.id === route.id
                            const metricValue = getMetricValue(route, selectedMetric)
                            return (
                              <Card 
                                key={route.id} 
                                className={`${isBest ? getMetricBgColor(selectedMetric) + ' border-2 ring-2 ring-offset-2 ' + (selectedMetric === 'cost' ? 'ring-green-500' : selectedMetric === 'time' ? 'ring-blue-500' : selectedMetric === 'carbon' ? 'ring-emerald-500' : 'ring-red-500') : 'bg-slate-50 dark:bg-slate-700'}`}
                              >
                                <CardContent className="p-3">
                                  <div className="flex items-start justify-between gap-2 mb-2">
                                    <div className="flex-1 min-w-0">
                                      <div className="flex items-center gap-2">
                                        <div className="text-sm font-medium truncate">{route.name}</div>
                                        {isBest && (
                                          <Badge className={`${getMetricBgColor(selectedMetric)} px-1.5 py-0.5 text-xs text-white`}>
                                            <Award className="h-3 w-3 mr-0.5 text-white" />
                                            Best
                                          </Badge>
                                        )}
                                      </div>
                                      <Badge variant="outline" className="text-xs mt-1">
                                        {route.type === 'combined' ? `${route.legs.length} legs` : 'Single leg'}
                                      </Badge>
                                    </div>
                                    <Button
                                      variant="ghost"
                                      size="sm"
                                      onClick={() => removeFromComparison(route.id)}
                                      className="h-6 w-6 p-0 text-slate-400 hover:text-red-600"
                                    >
                                      <X className="h-3 w-3" />
                                    </Button>
                                  </div>
                                  
                                  {/* Metric Display */}
                                  <div className="space-y-1.5 pt-2 border-t border-slate-200 dark:border-slate-600">
                                    <div className="flex items-center justify-between text-xs">
                                      <span className="text-slate-600 dark:text-slate-400 flex items-center gap-1">
                                        <DollarSign className="h-3 w-3" />
                                        Tariff Cost
                                      </span>
                                      <span className="font-semibold text-green-600 dark:text-green-400">
                                        {route.currency} {route.tariffCost.toFixed(2)}
                                      </span>
                                    </div>
                                    <div className="flex items-center justify-between text-xs">
                                      <span className="text-slate-600 dark:text-slate-400 flex items-center gap-1">
                                        {React.createElement(getMetricIcon(selectedMetric), { className: "h-3 w-3" })}
                                        <span className="capitalize">{selectedMetric}</span>
                                      </span>
                                      <span className={`font-semibold ${isBest ? getMetricColor(selectedMetric) : 'text-slate-700 dark:text-slate-300'}`}>
                                        {metricValue.toFixed(selectedMetric === 'risk' ? 1 : 0)} {getMetricUnit(selectedMetric)}
                                      </span>
                                    </div>
                                  </div>
                                </CardContent>
                              </Card>
                            )
                          })}
                        </div>

                        {/* Analytics Section */}
                        <div className="space-y-3 pt-3 border-t border-slate-200 dark:border-slate-600">
                          <h4 className="text-sm font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                            <BarChart3 className="h-4 w-4" />
                            Analytics
                          </h4>
                          
                          {/* Comparison Chart */}
                          <div className="grid grid-cols-1 gap-3">
                            {/* Bar Chart */}
                            <Card className="bg-white dark:bg-slate-800">
                              <CardContent className="p-4">
                                <div className="space-y-3">
                                  {selectedForComparison.map((route) => {
                                    const value = getMetricValue(route, selectedMetric)
                                    const maxValue = Math.max(...selectedForComparison.map(r => getMetricValue(r, selectedMetric)))
                                    const percentage = (value / maxValue) * 100
                                    const isBest = bestRoute?.id === route.id
                                    
                                    return (
                                      <div key={route.id} className="space-y-1">
                                        <div className="flex items-center justify-between text-xs">
                                          <span className="text-slate-700 dark:text-slate-300 truncate max-w-[200px]">
                                            {route.name}
                                          </span>
                                          <span className={`font-semibold ${isBest ? getMetricColor(selectedMetric) : ''}`}>
                                            {value.toFixed(selectedMetric === 'risk' ? 1 : 0)} {getMetricUnit(selectedMetric)}
                                          </span>
                                        </div>
                                        <div className="relative h-6 bg-slate-100 dark:bg-slate-700 rounded overflow-hidden">
                                          <div
                                            className={`absolute inset-y-0 left-0 transition-all duration-300 ${
                                              isBest 
                                                ? selectedMetric === 'cost' ? 'bg-green-500' : selectedMetric === 'time' ? 'bg-blue-500' : selectedMetric === 'carbon' ? 'bg-emerald-500' : 'bg-red-500'
                                                : 'bg-slate-300 dark:bg-slate-600'
                                            }`}
                                            style={{ width: `${percentage}%` }}
                                          >
                                            {isBest && (
                                              <div className="absolute inset-0 flex items-center justify-center">
                                                <Award className="h-3.5 w-3.5 text-white" />
                                              </div>
                                            )}
                                          </div>
                                        </div>
                                      </div>
                                    )
                                  })}
                                </div>
                              </CardContent>
                            </Card>

                            {/* Summary Statistics */}
                            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                              <Card className="bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
                                <CardContent className="p-3">
                                  <div className="flex items-center gap-2 mb-1">
                                    <DollarSign className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                                    <span className="text-xs text-slate-600 dark:text-slate-400">Total Cost</span>
                                  </div>
                                  <div className="text-lg font-bold text-blue-600 dark:text-blue-400">
                                    {selectedForComparison[0]?.currency} {totalTariffCost.toFixed(2)}
                                  </div>
                                </CardContent>
                              </Card>

                              <Card className="bg-purple-50 dark:bg-purple-900/20 border-purple-200 dark:border-purple-800">
                                <CardContent className="p-3">
                                  <div className="flex items-center gap-2 mb-1">
                                    <TrendingUp className="h-4 w-4 text-purple-600 dark:text-purple-400" />
                                    <span className="text-xs text-slate-600 dark:text-slate-400">Average Cost</span>
                                  </div>
                                  <div className="text-lg font-bold text-purple-600 dark:text-purple-400">
                                    {selectedForComparison[0]?.currency} {averageTariffCost.toFixed(2)}
                                  </div>
                                </CardContent>
                              </Card>

                              <Card className="bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800">
                                <CardContent className="p-3">
                                  <div className="flex items-center gap-2 mb-1">
                                    <Route className="h-4 w-4 text-green-600 dark:text-green-400" />
                                    <span className="text-xs text-slate-600 dark:text-slate-400">Total Legs</span>
                                  </div>
                                  <div className="text-lg font-bold text-green-600 dark:text-green-400">
                                    {selectedForComparison.reduce((sum, r) => sum + r.legs.length, 0)}
                                  </div>
                                </CardContent>
                              </Card>

                              <Card className="bg-amber-50 dark:bg-amber-900/20 border-amber-200 dark:border-amber-800">
                                <CardContent className="p-3">
                                  <div className="flex items-center gap-2 mb-1">
                                    <MapPin className="h-4 w-4 text-amber-600 dark:text-amber-400" />
                                    <span className="text-xs text-slate-600 dark:text-slate-400">Countries</span>
                                  </div>
                                  <div className="text-lg font-bold text-amber-600 dark:text-amber-400">
                                    {new Set(selectedForComparison.flatMap(r => r.legs.flatMap(l => [l.sourceCountry, l.destinationCountry]))).size}
                                  </div>
                                </CardContent>
                              </Card>
                            </div>
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* World Map - Toggleable */}
            {showMap && (
              <div className="flex-1 overflow-hidden bg-white dark:bg-slate-900">
                <WorldMap optimalRoutesData={optimalRoutesData} />
              </div>
            )}
          </div>
        </div>
      </div>
    </ProtectedRoute>
  )
}
