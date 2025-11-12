"use client"

import React, { useState, useEffect, useMemo, useRef } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import WorldMap, { type OptimalRoutesData } from "@/components/WorldMap"
import TopBar from "@/components/TopBar"
import { ProtectedRoute } from "@/components/ProtectedRoute"
import { ArrowLeft, Route, MapPin, DollarSign, Clock, Leaf, AlertTriangle, GripVertical, X, BarChart3, TrendingUp, Award, Map, Download } from "lucide-react"
import { useRouter } from "next/navigation"
import { getOptimalRoutes, COUNTRY_COORDINATES, getCountries } from "@/lib/api"
import { domToPng } from 'modern-screenshot'
import jsPDF from "jspdf"

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
  const [isExporting, setIsExporting] = useState<boolean>(false)
  const [builderLegs, setBuilderLegs] = useState<CalculationHistory[]>([])
  const [builderDropTarget, setBuilderDropTarget] = useState<boolean>(false)
  const [countries, setCountries] = useState<Array<{ code: string; name: string }>>([])

  // Refs for PDF export
  const analyticsRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<HTMLDivElement>(null)

  // Fetch countries on component mount
  useEffect(() => {
    const fetchCountries = async () => {
      try {
        const { ok, data } = await getCountries()
        if (ok && data.countries) {
          setCountries(data.countries)
        }
      } catch (error) {
        console.error('Error fetching countries:', error)
      }
    }
    
    fetchCountries()
  }, [])

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

  // Route Builder Functions
  const handleDropOnBuilder = (e: React.DragEvent) => {
    e.preventDefault()
    if (draggedItem && draggedItem.type === 'single') {
      const calc = draggedItem.legs[0]
      // Check if the leg connects properly
      if (builderLegs.length > 0) {
        const lastLeg = builderLegs[builderLegs.length - 1]
        if (lastLeg.destinationCountry !== calc.sourceCountry) {
          alert(`Cannot add this leg: Last destination was ${lastLeg.destinationCountry}, but this leg starts from ${calc.sourceCountry}`)
          setBuilderDropTarget(false)
          return
        }
      }
      setBuilderLegs([...builderLegs, calc])
    }
    setBuilderDropTarget(false)
  }

  const handleBuilderDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setBuilderDropTarget(true)
  }

  const handleBuilderDragLeave = () => {
    setBuilderDropTarget(false)
  }

  const removeLegFromBuilder = (legId: string) => {
    setBuilderLegs(builderLegs.filter(leg => leg.id !== legId))
  }

  const saveBuiltRoute = () => {
    if (builderLegs.length < 2) {
      alert("A combined route must have at least 2 legs")
      return
    }

    const totalCost = builderLegs.reduce((sum, leg) => sum + leg.tariffAmount, 0)
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
    // Update localStorage
    localStorage.setItem('combinedRoutes', JSON.stringify([newRoute, ...combinedRoutes]))
    setBuilderLegs([])
  }

  // Helper to find country code from country name
  const getCountryCodeFromName = (countryName: string): string | null => {
    const country = countries.find(c => c.name === countryName)
    return country ? country.code : null
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

  // Calculate optimal routes for the best route based on selected metric
  useEffect(() => {
    const calculateOptimalRoutes = async () => {
      if (!bestRoute || selectedForComparison.length === 0) {
        setOptimalRoutesData(null)
        return
      }

      console.log(`🚢 Calculating optimal routes for best ${selectedMetric} route:`, bestRoute.name)

      // Collect routes grouped by optimization type
      const routesByOptimization: { 
        [key: string]: { 
          coordinates: number[][], 
          metrics: {
            distance_km: number,
            cost_usd: number,
            time_hours: number,
            co2_kg: number,
            risk_score: number,
            transport_type: string
          }
        } 
      } = {}

      for (let i = 0; i < bestRoute.legs.length; i++) {
        const leg = bestRoute.legs[i]
        const sourceCode = getCountryCodeFromName(leg.sourceCountry)
        const destCode = getCountryCodeFromName(leg.destinationCountry)

        if (!sourceCode || !destCode) {
          console.warn(`⚠️ Could not find country codes for leg ${i + 1}:`, leg.sourceCountry, leg.destinationCountry)
          continue
        }

        const sourceCoords = COUNTRY_COORDINATES[sourceCode]
        const destCoords = COUNTRY_COORDINATES[destCode]

        if (!sourceCoords || !destCoords) {
          console.warn(`⚠️ No coordinates found for leg ${i + 1}`)
          continue
        }

        try {
          const { ok, data } = await getOptimalRoutes({
            src_lat: sourceCoords.lat,
            src_lon: sourceCoords.lon,
            dst_lat: destCoords.lat,
            dst_lon: destCoords.lon,
          })

          if (ok && data.features) {
            const features = data.features as Array<Record<string, unknown>>
            
            features.forEach((feature: Record<string, unknown>) => {
              const props = feature.properties as Record<string, unknown>
              const optType = props?.optimization_type as string | undefined
              const geometry = feature.geometry as { type: string, coordinates: number[][] | number[][][] }
              
              if (optType) {
                if (!routesByOptimization[optType]) {
                  routesByOptimization[optType] = {
                    coordinates: [],
                    metrics: {
                      distance_km: 0,
                      cost_usd: 0,
                      time_hours: 0,
                      co2_kg: 0,
                      risk_score: 0,
                      transport_type: props.transport_type as string || 'SEA'
                    }
                  }
                }

                // Concatenate coordinates
                if (geometry.type === 'LineString') {
                  routesByOptimization[optType].coordinates.push(...(geometry.coordinates as number[][]))
                } else if (geometry.type === 'MultiLineString') {
                  (geometry.coordinates as number[][][]).forEach(segment => {
                    routesByOptimization[optType].coordinates.push(...segment)
                  })
                }

                // Sum up metrics
                routesByOptimization[optType].metrics.distance_km += (props.distance_km as number) || 0
                routesByOptimization[optType].metrics.cost_usd += (props.cost_usd as number) || 0
                routesByOptimization[optType].metrics.time_hours += (props.time_hours as number) || 0
                routesByOptimization[optType].metrics.co2_kg += (props.co2_kg as number) || 0
                routesByOptimization[optType].metrics.risk_score += (props.risk_score as number) || 0
              }
            })
          }
        } catch (error) {
          console.error(`❌ Error calculating leg ${i + 1}:`, error)
        }
      }

      // Transform to OptimalRoutesData format
      const transformedData: OptimalRoutesData = {}
      
      Object.entries(routesByOptimization).forEach(([optType, route]) => {
        if (route.coordinates.length > 0) {
          transformedData[optType as keyof OptimalRoutesData] = {
            coordinates: route.coordinates,
            geometry: {
              type: 'LineString',
              coordinates: route.coordinates
            },
            metrics: route.metrics,
            optimization: optType.replace('_optimized', '')
          }
        }
      })

      console.log('✅ Optimal routes calculated:', transformedData)
      setOptimalRoutesData(transformedData)
    }

    calculateOptimalRoutes()
  }, [bestRoute, selectedMetric, selectedForComparison.length])

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

  // PDF Export Function
  const exportToPDF = async () => {
    if (!analyticsRef.current || selectedForComparison.length === 0) return

    setIsExporting(true)

    try {
      // Show map if not already visible
      const wasMapShown = showMap
      if (!wasMapShown) {
        setShowMap(true)
        // Wait for map to fully render
        await new Promise(resolve => setTimeout(resolve, 2000))
      }

      // Create PDF in landscape for better layout
      const pdf = new jsPDF({
        orientation: 'landscape',
        unit: 'mm',
        format: 'a4',
        compress: true // Enable PDF compression
      })

      const pageWidth = 297 // A4 landscape width in mm
      const pageHeight = 210 // A4 landscape height in mm
      const margin = 15

      // Add title page
      pdf.setFontSize(24)
      pdf.setFont('helvetica', 'bold')
      pdf.text('Route Comparison Report', 148.5, 40, { align: 'center' })
      
      pdf.setFontSize(12)
      pdf.setFont('helvetica', 'normal')
      pdf.text(`Generated: ${new Date().toLocaleDateString()} ${new Date().toLocaleTimeString()}`, 148.5, 50, { align: 'center' })
      pdf.text(`Optimization: ${selectedMetric.toUpperCase()}`, 148.5, 57, { align: 'center' })
      pdf.text(`Routes Compared: ${selectedForComparison.length}`, 148.5, 64, { align: 'center' })

      // Page 2: Route Summaries
      pdf.addPage()
      pdf.setFontSize(18)
      pdf.setFont('helvetica', 'bold')
      pdf.text('Route Summaries', margin, margin + 5)

      let yPos = margin + 15

      selectedForComparison.forEach((route, index) => {
        const isBest = bestRoute?.id === route.id
        
        // Check if we need a new page
        if (yPos > pageHeight - 60) {
          pdf.addPage()
          yPos = margin + 5
        }

        // Route header with number
        pdf.setFontSize(14)
        pdf.setFont('helvetica', 'bold')
        pdf.setTextColor(31, 41, 55) // slate-900
        const routeTitle = `Route ${index + 1}: ${route.name.replace(/→/g, 'to').replace(/->/g, 'to')}`
        pdf.text(routeTitle, margin, yPos)
        yPos += 7
        
        // Best route indicator on new line
        if (isBest) {
          pdf.setFontSize(10)
          pdf.setTextColor(5, 150, 105) // green-600
          pdf.setFont('helvetica', 'bold')
          pdf.text('[OPTIMAL ROUTE]', margin, yPos)
          pdf.setFont('helvetica', 'normal')
          yPos += 6
        }

        // Route type
        pdf.setFontSize(10)
        pdf.setFont('helvetica', 'normal')
        pdf.setTextColor(100, 116, 139) // slate-500
        pdf.text(`Type: ${route.type === 'combined' ? `Multi-leg route (${route.legs.length} stops)` : 'Direct route'}`, margin, yPos)
        yPos += 8

        // Metrics table header
        pdf.setFontSize(10)
        pdf.setFont('helvetica', 'bold')
        pdf.setTextColor(255, 255, 255) // white text
        
        const col1 = margin + 3
        const col2 = margin + 65
        const col3 = margin + 135
        const col4 = margin + 205

        // Header row with dark background
        pdf.setFillColor(71, 85, 105) // slate-600
        pdf.rect(margin, yPos - 4, pageWidth - 2 * margin, 7, 'F')
        pdf.text('Metric', col1, yPos)
        pdf.text('Value', col2, yPos)
        pdf.text('Details', col3, yPos)
        pdf.text('Status', col4, yPos)
        yPos += 7

        pdf.setFont('helvetica', 'normal')
        pdf.setTextColor(31, 41, 55) // slate-900
        
        // Alternating row backgrounds
        let rowBg = true
        
        // Tariff Cost
        if (rowBg) {
          pdf.setFillColor(248, 250, 252) // slate-50
          pdf.rect(margin, yPos - 4, pageWidth - 2 * margin, 6, 'F')
        }
        pdf.setFont('helvetica', 'bold')
        pdf.text('Tariff Cost', col1, yPos)
        pdf.setFont('helvetica', 'normal')
        pdf.text(`${route.currency} ${route.tariffCost.toFixed(2)}`, col2, yPos)
        pdf.setFontSize(9)
        pdf.setTextColor(100, 116, 139)
        pdf.text('Total customs duties', col3, yPos)
        pdf.setFontSize(10)
        pdf.setTextColor(31, 41, 55)
        const costDiff = parseFloat(((route.tariffCost / averageTariffCost - 1) * 100).toFixed(0))
        if (!isNaN(costDiff) && isFinite(costDiff)) {
          pdf.setTextColor(costDiff > 0 ? 220 : 5, costDiff > 0 ? 38 : 150, costDiff > 0 ? 38 : 105)
          pdf.text(costDiff > 0 ? `+${costDiff}%` : `${costDiff}%`, col4, yPos)
        } else {
          pdf.setTextColor(100, 116, 139)
          pdf.text('Baseline', col4, yPos)
        }
        pdf.setTextColor(31, 41, 55)
        yPos += 6
        rowBg = !rowBg

        // Transit Time
        if (rowBg) {
          pdf.setFillColor(248, 250, 252)
          pdf.rect(margin, yPos - 4, pageWidth - 2 * margin, 6, 'F')
        }
        const estimatedTime = route.legs.length * 24
        pdf.setFont('helvetica', 'bold')
        pdf.text('Transit Time', col1, yPos)
        pdf.setFont('helvetica', 'normal')
        pdf.text(`${estimatedTime} hours`, col2, yPos)
        pdf.setFontSize(9)
        pdf.setTextColor(100, 116, 139)
        pdf.text(`Approx. ${(estimatedTime / 24).toFixed(1)} days`, col3, yPos)
        pdf.setFontSize(10)
        pdf.setTextColor(31, 41, 55)
        yPos += 6
        rowBg = !rowBg

        // Carbon Footprint
        if (rowBg) {
          pdf.setFillColor(248, 250, 252)
          pdf.rect(margin, yPos - 4, pageWidth - 2 * margin, 6, 'F')
        }
        const carbonEmissions = route.legs.length * 100
        pdf.setFont('helvetica', 'bold')
        pdf.text('Carbon Footprint', col1, yPos)
        pdf.setFont('helvetica', 'normal')
        pdf.text(`${carbonEmissions} kg CO2`, col2, yPos)
        pdf.setFontSize(9)
        pdf.setTextColor(100, 116, 139)
        pdf.text('Estimated emissions', col3, yPos)
        pdf.setFontSize(10)
        pdf.setTextColor(31, 41, 55)
        yPos += 6
        rowBg = !rowBg

        // Risk Score
        if (rowBg) {
          pdf.setFillColor(248, 250, 252)
          pdf.rect(margin, yPos - 4, pageWidth - 2 * margin, 6, 'F')
        }
        const riskScore = Math.min(route.legs.length * 2, 10)
        pdf.setFont('helvetica', 'bold')
        pdf.text('Risk Score', col1, yPos)
        pdf.setFont('helvetica', 'normal')
        pdf.text(`${riskScore.toFixed(1)} / 10`, col2, yPos)
        pdf.setFontSize(9)
        pdf.setTextColor(100, 116, 139)
        const riskLevel = riskScore < 4 ? 'Low risk' : riskScore < 7 ? 'Moderate risk' : 'High risk'
        pdf.text(riskLevel, col3, yPos)
        pdf.setFontSize(10)
        pdf.setTextColor(31, 41, 55)
        yPos += 6
        rowBg = !rowBg

        // Transport Mode
        if (rowBg) {
          pdf.setFillColor(248, 250, 252)
          pdf.rect(margin, yPos - 4, pageWidth - 2 * margin, 6, 'F')
        }
        pdf.setFont('helvetica', 'bold')
        pdf.text('Transport Mode', col1, yPos)
        pdf.setFont('helvetica', 'normal')
        pdf.text('Sea Freight', col2, yPos)
        pdf.setFontSize(9)
        pdf.setTextColor(100, 116, 139)
        pdf.text('Primary shipping method', col3, yPos)
        pdf.setFontSize(10)
        pdf.setTextColor(31, 41, 55)
        yPos += 9

        // Goods Shipped section
        pdf.setFontSize(10)
        pdf.setFont('helvetica', 'bold')
        pdf.setTextColor(71, 85, 105)
        pdf.text('Goods Shipped:', margin, yPos)
        yPos += 5
        pdf.setFont('helvetica', 'normal')
        pdf.setFontSize(9)
        pdf.setTextColor(31, 41, 55)
        const goodsText = `${route.legs[0].productCode} - ${route.legs[0].productDescription}`
        const maxGoodsLength = 100
        if (goodsText.length > maxGoodsLength) {
          pdf.text(goodsText.substring(0, maxGoodsLength - 3) + '...', margin + 3, yPos)
        } else {
          pdf.text(goodsText, margin + 3, yPos)
        }
        yPos += 7

        // Route Path section
        pdf.setFontSize(10)
        pdf.setFont('helvetica', 'bold')
        pdf.setTextColor(71, 85, 105)
        pdf.text('Route Path:', margin, yPos)
        yPos += 5
        
        pdf.setFont('helvetica', 'normal')
        pdf.setFontSize(9)
        pdf.setTextColor(31, 41, 55)
        
        if (route.legs.length > 1) {
          route.legs.forEach((leg, legIndex) => {
            pdf.text(`  ${legIndex + 1}. ${leg.sourceCountry} to ${leg.destinationCountry}`, margin + 3, yPos)
            yPos += 4
          })
          yPos += 2
        } else {
          pdf.text(`  ${route.legs[0].sourceCountry} to ${route.legs[0].destinationCountry} (Direct)`, margin + 3, yPos)
          yPos += 6
        }

        // Separator line
        pdf.setDrawColor(203, 213, 225) // slate-300
        pdf.setLineWidth(0.5)
        pdf.line(margin, yPos, pageWidth - margin, yPos)
        yPos += 10
      })

      // Analytics Section - Capture and split intelligently
      pdf.addPage()
      
      try {
        console.log('📊 Capturing analytics...')
        
        // Add a temporary class to force light colors for PDF
        const analyticsElement = analyticsRef.current
        analyticsElement.classList.add('pdf-export-mode')
        
        // Add temporary styles for PDF export with better font sizing
        const styleTag = document.createElement('style')
        styleTag.id = 'pdf-export-styles'
        styleTag.textContent = `
          .pdf-export-mode {
            background-color: #ffffff !important;
            padding: 20px !important;
          }
          .pdf-export-mode * {
            color: #0f172a !important;
            border-color: #cbd5e1 !important;
          }
          .pdf-export-mode .bg-green-50,
          .pdf-export-mode .bg-blue-50,
          .pdf-export-mode .bg-emerald-50,
          .pdf-export-mode .bg-red-50,
          .pdf-export-mode .bg-slate-50 {
            background-color: #f8fafc !important;
          }
          .pdf-export-mode .bg-white {
            background-color: #ffffff !important;
          }
          .pdf-export-mode .text-green-600 { color: #059669 !important; }
          .pdf-export-mode .text-blue-600 { color: #2563eb !important; }
          .pdf-export-mode .text-emerald-600 { color: #059669 !important; }
          .pdf-export-mode .text-red-600 { color: #dc2626 !important; }
          .pdf-export-mode .text-slate-600 { color: #475569 !important; }
          .pdf-export-mode .text-slate-900 { color: #0f172a !important; }
          .pdf-export-mode .text-slate-100 { color: #0f172a !important; }
          .pdf-export-mode .border-green-200 { border-color: #bbf7d0 !important; }
          .pdf-export-mode .border-blue-200 { border-color: #bfdbfe !important; }
          .pdf-export-mode .border-emerald-200 { border-color: #a7f3d0 !important; }
          .pdf-export-mode .border-red-200 { border-color: #fecaca !important; }
          /* Increase font sizes for better readability */
          .pdf-export-mode .text-xs { font-size: 0.875rem !important; }
          .pdf-export-mode .text-sm { font-size: 1rem !important; }
          .pdf-export-mode .text-lg { font-size: 1.25rem !important; }
        `
        document.head.appendChild(styleTag)
        
        // Wait for styles to apply
        await new Promise(resolve => setTimeout(resolve, 200))
        
        // Calculate dimensions first
        const availableWidth = pageWidth - (2 * margin)
        const availableHeight = pageHeight - (2 * margin) - 15 // Extra space for title
        
        // Get element dimensions
        const elementWidth = analyticsElement.offsetWidth
        const elementHeight = analyticsElement.offsetHeight
        
        // Calculate optimal scale: balance between quality and file size
        const targetWidthPx = availableWidth * 3.78 // mm to px at 96 DPI
        const optimalScale = Math.max(1.5, Math.min(2.5, targetWidthPx / elementWidth))
        
        console.log(`📐 Element: ${elementWidth}x${elementHeight}px, Scale: ${optimalScale.toFixed(2)}`)
        
        // Use modern-screenshot with optimal settings
        const dataUrl = await domToPng(analyticsElement, {
          quality: 0.95,
          backgroundColor: '#ffffff',
          scale: optimalScale,
          fetch: {
            requestInit: {
              mode: 'cors',
              credentials: 'omit'
            }
          }
        })
        
        // Clean up
        analyticsElement.classList.remove('pdf-export-mode')
        document.head.removeChild(styleTag)

        // Load image to get dimensions
        const img = new Image()
        img.src = dataUrl
        await new Promise((resolve) => { img.onload = resolve })
        
        console.log(`🖼️ Captured image: ${img.width}x${img.height}px`)
        
        // Calculate dimensions to fit full width
        const fullWidth = availableWidth
        const fullHeight = (img.height * fullWidth) / img.width
        const maxHeightPerPage = availableHeight
        
        // Determine how many pages we need with better spacing to avoid cutting charts
        const numPages = Math.ceil(fullHeight / maxHeightPerPage)
        console.log(`📄 Splitting across ${numPages} page(s), height: ${fullHeight.toFixed(1)}mm`)
        
        // Split image across pages with intelligent breaks
        for (let i = 0; i < numPages; i++) {
          if (i > 0) {
            pdf.addPage()
          }
          
          // Add title
          pdf.setFontSize(16)
          pdf.setFont('helvetica', 'bold')
          pdf.setTextColor(31, 41, 55)
          const title = i === 0 ? 'Visual Analytics & Metrics' : `Visual Analytics & Metrics (Page ${i + 1}/${numPages})`
          pdf.text(title, margin, margin + 5)
          
          // Calculate portion of image to show
          // Add 5% overlap between pages to ensure no content is lost
          const overlap = i > 0 ? maxHeightPerPage * 0.05 : 0
          const startY = i * maxHeightPerPage - overlap
          const segmentHeight = Math.min(maxHeightPerPage, fullHeight - startY)
          
          // Calculate source rectangle in image coordinates
          const srcY = (startY / fullHeight) * img.height
          const srcHeight = (segmentHeight / fullHeight) * img.height
          
          // Create canvas for this segment
          const canvas = document.createElement('canvas')
          canvas.width = img.width
          canvas.height = Math.ceil(srcHeight)
          const ctx = canvas.getContext('2d')
          
          if (ctx) {
            // Fill with white background
            ctx.fillStyle = '#ffffff'
            ctx.fillRect(0, 0, canvas.width, canvas.height)
            
            // Draw the segment
            ctx.drawImage(
              img,
              0, srcY, img.width, srcHeight,  // Source rectangle
              0, 0, img.width, srcHeight       // Destination rectangle
            )
            
            const segmentData = canvas.toDataURL('image/jpeg', 0.92)
            pdf.addImage(segmentData, 'JPEG', margin, margin + 10, fullWidth, segmentHeight, undefined, 'FAST')
          }
        }
        
        console.log('✅ Analytics captured successfully')
      } catch (error) {
        console.error('❌ Error capturing analytics:', error)
        pdf.setFontSize(12)
        pdf.setTextColor(220, 38, 38)
        pdf.text('Analytics could not be captured: ' + (error as Error).message, margin, margin + 20)
      }

      // Map removed from PDF export for simplicity

      // Save PDF
      const fileName = `route-comparison-${selectedMetric}-${new Date().toISOString().split('T')[0]}.pdf`
      pdf.save(fileName)

      console.log('✅ PDF exported successfully:', fileName)
    } catch (error) {
      console.error('❌ Error exporting PDF:', error)
      alert('Failed to export PDF. Please try again.')
    } finally {
      setIsExporting(false)
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

          {/* Route Builder Section */}
          <div className="p-4 border-t border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900">
            <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-2 flex items-center gap-2">
              <Route className="h-4 w-4" />
              Route Builder
            </h3>
            <p className="text-xs text-slate-600 dark:text-slate-400 mb-3">
              Drag single routes here to combine them into multi-leg routes
            </p>
            <div
              onDrop={handleDropOnBuilder}
              onDragOver={handleBuilderDragOver}
              onDragLeave={handleBuilderDragLeave}
              className={`min-h-[120px] border-2 border-dashed rounded-lg p-3 transition-colors ${
                builderDropTarget
                  ? 'border-amber-500 bg-amber-100 dark:bg-amber-900/20'
                  : 'border-amber-300 dark:border-amber-700 bg-white dark:bg-slate-800'
              }`}
            >
              {builderLegs.length === 0 ? (
                <div className="text-center py-6 text-amber-600 dark:text-amber-400 text-sm">
                  Drop single routes here to build a multi-leg route
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
                      onClick={saveBuiltRoute}
                      size="sm"
                      className="bg-amber-600 hover:bg-amber-700 text-white"
                    >
                      Save Route
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Panel - Comparison & Map */}
        <div className="flex-1 flex flex-col min-w-0">
          <TopBar sidebarOpen={false} onToggleSidebar={() => {}} />

          <div className="flex-1 flex flex-col overflow-hidden">
            {/* Comparison Drop Zone - Now Scrollable */}
            <div className="flex-1 overflow-y-auto p-4 bg-slate-50 dark:bg-slate-900">
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
                      <div className="flex items-center gap-2">
                        <Button
                          variant="default"
                          size="sm"
                          onClick={exportToPDF}
                          disabled={isExporting}
                          className="bg-blue-600 hover:bg-blue-700 text-white gap-2"
                        >
                          <Download className="h-4 w-4" />
                          {isExporting ? 'Exporting...' : 'Export PDF'}
                        </Button>
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
                    </div>

                    {/* Metric Selector */}
                    <div className="flex gap-2 pb-3 border-b border-slate-200 dark:border-slate-600 flex-wrap">
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
                        {showMap ? 'Hide Map Reference' : 'Show Map Reference'}
                      </Button>
                    </div>

                    {/* Analytics View - Wrapped for PDF export */}
                    <div ref={analyticsRef}>
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
                            Route Comparison - All Metrics
                          </h4>
                          
                          {/* Comparison Chart */}
                          <div className="grid grid-cols-1 gap-3">
                            {/* Grouped Bar Chart */}
                            <Card className="bg-white dark:bg-slate-800">
                              <CardContent className="p-4">
                                <div className="space-y-6">
                                  {/* Chart for each metric */}
                                  {(['cost', 'time', 'carbon', 'risk'] as MetricType[]).map((metric) => {
                                    const Icon = getMetricIcon(metric)
                                    const maxValue = Math.max(...selectedForComparison.map(r => getMetricValue(r, metric)))
                                    
                                    return (
                                      <div key={metric} className="space-y-2">
                                        {/* Metric Header */}
                                        <div className="flex items-center gap-2 pb-1 border-b border-slate-200 dark:border-slate-600">
                                          <Icon className={`h-4 w-4 ${getMetricColor(metric)}`} />
                                          <span className="text-sm font-semibold text-slate-900 dark:text-slate-100 capitalize">
                                            {metric}
                                          </span>
                                          <span className="text-xs text-slate-500 dark:text-slate-400">
                                            ({getMetricUnit(metric)})
                                          </span>
                                        </div>
                                        
                                        {/* Grouped Bars */}
                                        <div className="flex items-end gap-2 h-40 border-b border-slate-200 dark:border-slate-700">
                                          {selectedForComparison.map((route) => {
                                            const value = getMetricValue(route, metric)
                                            const heightPercentage = (value / maxValue) * 100
                                            const heightPixels = Math.max((heightPercentage / 100) * 140, 20) // Convert to pixels with min 20px
                                            const isBest = bestRoute?.id === route.id && selectedMetric === metric
                                            
                                            return (
                                              <div key={route.id} className="flex-1 flex flex-col items-center gap-2">
                                                {/* Value Label */}
                                                <div className={`text-xs font-semibold ${isBest ? getMetricColor(metric) : 'text-slate-600 dark:text-slate-400'}`}>
                                                  {value.toFixed(metric === 'risk' ? 1 : 0)}
                                                </div>
                                                
                                                {/* Bar */}
                                                <div 
                                                  className={`relative w-full rounded-t overflow-hidden transition-all duration-300 ${
                                                    metric === 'cost' 
                                                      ? 'bg-green-500 hover:bg-green-600' 
                                                      : metric === 'time' 
                                                      ? 'bg-blue-500 hover:bg-blue-600' 
                                                      : metric === 'carbon' 
                                                      ? 'bg-emerald-500 hover:bg-emerald-600' 
                                                      : 'bg-red-500 hover:bg-red-600'
                                                  } ${isBest ? 'ring-2 ring-yellow-400' : ''}`}
                                                  style={{ height: `${heightPixels}px` }}
                                                >
                                                  {isBest && (
                                                    <div className="absolute top-1 left-1/2 -translate-x-1/2">
                                                      <Award className="h-3 w-3 text-white drop-shadow" />
                                                    </div>
                                                  )}
                                                </div>
                                                
                                                {/* Route Name Label */}
                                                <div className="text-[10px] text-slate-600 dark:text-slate-400 text-center leading-tight max-w-full px-1">
                                                  <div className="truncate" title={route.name}>
                                                    {route.name.split('→').map((part, i) => (
                                                      <span key={i}>
                                                        {part.trim().substring(0, 3)}
                                                        {i < route.name.split('→').length - 1 && '→'}
                                                      </span>
                                                    ))}
                                                  </div>
                                                </div>
                                              </div>
                                            )
                                          })}
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
                      </div>
                  </div>
                )}
              </div>
            </div>

            {/* Map Sidebar - Slide from right */}
            <div 
              ref={mapRef}
              className={`fixed top-0 right-0 h-screen w-[900px] bg-white dark:bg-slate-900 shadow-2xl transform transition-transform duration-300 ease-in-out z-50 ${
              showMap ? 'translate-x-0' : 'translate-x-full'
            }`}>
              <div className="h-full flex flex-col">
                {/* Map Header */}
                <div className="p-4 border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800">
                  <div className="flex items-center justify-between mb-2">
                    <h3 className="text-lg font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-2">
                      <Map className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                      Route Map Reference
                    </h3>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setShowMap(false)}
                      className="text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-slate-100"
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                  <p className="text-xs text-slate-600 dark:text-slate-400">
                    Showing optimal {selectedMetric}-optimized routes for selected calculations
                  </p>
                </div>

                {/* Map Container */}
                <div className="flex-1 overflow-hidden">
                  <WorldMap 
                    optimalRoutesData={optimalRoutesData}
                    routeDetails={bestRoute ? {
                      productCode: bestRoute.legs[0]?.productCode,
                      productDescription: bestRoute.legs[0]?.productDescription,
                      tariffAmount: bestRoute.tariffCost,
                      currency: bestRoute.currency,
                      sourceCountry: bestRoute.legs[0]?.sourceCountry,
                      destinationCountry: bestRoute.legs[bestRoute.legs.length - 1]?.destinationCountry
                    } : undefined}
                  />
                </div>
              </div>
            </div>

            {/* Overlay backdrop when map is open */}
            {showMap && (
              <div 
                className="fixed inset-0 bg-black/20 backdrop-blur-sm z-40 transition-opacity duration-300"
                onClick={() => setShowMap(false)}
              />
            )}
          </div>
        </div>
      </div>
    </ProtectedRoute>
  )
}
