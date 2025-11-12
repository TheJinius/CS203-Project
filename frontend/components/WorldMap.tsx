import { useEffect, useRef, useState } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import { Label } from "@/components/ui/label"
import { Button } from "@/components/ui/button"
import { X, Package, DollarSign, Clock, AlertTriangle, Leaf, Truck } from "lucide-react"
import { useTheme } from "@/contexts/ThemeContext"

// GeoJSON types
interface GeoJSONGeometry {
  type: string
  coordinates: number[][] | number[][][]
}

interface GeoJSONFeature {
  type: string
  geometry: GeoJSONGeometry
  properties?: Record<string, unknown>
}

export interface GeoJSONData {
  type: string
  features: GeoJSONFeature[]
}

// Mapbox type declarations
declare global {
  interface Window {
    mapboxgl?: {
      accessToken: string
      Map: new (options: Record<string, unknown>) => {
        loaded: () => boolean
        on: (event: string, callback: () => void) => void
        getLayer: (id: string) => unknown
        removeLayer: (id: string) => void
        getSource: (id: string) => unknown
        removeSource: (id: string) => void
        addSource: (id: string, source: Record<string, unknown>) => void
        addLayer: (layer: Record<string, unknown>) => void
        addControl: (control: unknown, position?: string) => void
        fitBounds: (bounds: unknown, options?: Record<string, unknown>) => void
      }
      LngLatBounds: new () => {
        extend: (coord: [number, number]) => void
        isEmpty: () => boolean
      }
      NavigationControl: new () => unknown
    }
  }
}

interface RouteMetrics {
  distance_km: number
  cost_usd: number
  time_hours: number
  co2_kg: number
  risk_score: number
  transport_type: string
  [key: string]: unknown
}

interface OptimalRoute {
  coordinates: number[][]
  geometry?: {
    type: string
    coordinates: number[][] | number[][][]
  }
  metrics: RouteMetrics
  optimization: string
}

export interface OptimalRoutesData {
  cost_optimized?: OptimalRoute
  time_optimized?: OptimalRoute
  risk_optimized?: OptimalRoute
  co2_optimized?: OptimalRoute
}

interface WorldMapProps {
  geojsonData?: GeoJSONData | null
  optimalRoutesData?: OptimalRoutesData | null
  routeDetails?: {
    productCode?: string
    productDescription?: string
    tariffAmount?: number
    currency?: string
    sourceCountry?: string
    destinationCountry?: string
  }
}

// Route configuration with colors and labels
const ROUTE_CONFIG = {
  cost_optimized: {
    color: "#a8a232",
    label: "Cost optimised",
  },
  time_optimized: {
    color: "#3b82f6",
    label: "Time optimised",
  },
  risk_optimized: {
    color: "#a855f7",
    label: "Risk optimised",
  },
  co2_optimized: {
    color: "#10b981",
    label: "CO2 optimised",
  }
}

// More robust antimeridian fix using proper geodesic logic
function fixAntimeridianWrapping(geojsonData: GeoJSONData | undefined): GeoJSONData | undefined {
  if (!geojsonData?.features?.[0]?.geometry?.coordinates) {
    return geojsonData
  }

  const feature = geojsonData.features[0]
  const coords = feature.geometry.coordinates as number[][]
  
  // Check if it's already a MultiLineString
  if (feature.geometry.type === 'MultiLineString') {
    return geojsonData
  }

  const segments: number[][][] = []
  let currentSegment: number[][] = []
  
  for (let i = 0; i < coords.length; i++) {
    const currentPoint = coords[i] as number[]
    
    if (i === 0) {
      currentSegment.push(currentPoint)
      continue
    }
    
    const prevPoint = coords[i - 1] as number[]
    const prevLon = prevPoint[0]
    const currLon = currentPoint[0]
    
    // Calculate the shortest distance considering wrapping
    let lonDiff = currLon - prevLon
    
    // Normalize to -180 to 180
    while (lonDiff > 180) lonDiff -= 360
    while (lonDiff < -180) lonDiff += 360
    
    // If we're jumping more than 180 degrees, we're crossing the antimeridian
    const crossesAntimeridian = Math.abs(currLon - prevLon) > 180
    
    if (crossesAntimeridian) {
      // Close current segment
      if (currentSegment.length > 1) {
        segments.push([...currentSegment])
      }
      // Start new segment with current point
      currentSegment = [currentPoint]
    } else {
      currentSegment.push(currentPoint)
    }
  }
  
  // Add the last segment
  if (currentSegment.length > 1) {
    segments.push(currentSegment)
  }

  // If we have multiple segments, return as MultiLineString
  if (segments.length > 1) {
    console.log(`🌍 Split route into ${segments.length} segments to avoid wrapping`)
    return {
      ...geojsonData,
      features: [{
        ...feature,
        geometry: {
          type: 'MultiLineString',
          coordinates: segments
        }
      }]
    }
  }

  console.log('🌍 Route does not cross antimeridian')
  return geojsonData
}

export default function WorldMap({ geojsonData, optimalRoutesData, routeDetails }: WorldMapProps) {
  const mapContainer = useRef<HTMLDivElement>(null)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const map = useRef<any>(null)
  const { theme } = useTheme()
  
  // Track which routes are visible
  const [visibleRoutes, setVisibleRoutes] = useState<{[key: string]: boolean}>({
    cost_optimized: true,
    time_optimized: true,
    risk_optimized: true,
    co2_optimized: true
  })

  // Track selected route for popup
  const [selectedRoute, setSelectedRoute] = useState<{
    type: string
    label: string
    metrics: RouteMetrics
    routeDetails?: typeof routeDetails
  } | null>(null)

  useEffect(() => {
    if (map.current) return

    const loadMapbox = async () => {
      if (!document.querySelector('link[href*="mapbox-gl"]')) {
        const link = document.createElement("link")
        link.href = "https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css"
        link.rel = "stylesheet"
        document.head.appendChild(link)
      }

      if (!window.mapboxgl) {
        const script = document.createElement("script")
        script.src = "https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js"
        script.onload = initializeMap
        document.head.appendChild(script)
      } else {
        initializeMap()
      }
    }

    const initializeMap = () => {
      if (!window.mapboxgl || !mapContainer.current) return

      window.mapboxgl.accessToken = process.env.NEXT_PUBLIC_MAPBOX_TOKEN || ""

      // Use light or dark style based on theme
      const mapStyle = theme === "light" 
        ? "mapbox://styles/mapbox/light-v11" 
        : "mapbox://styles/mapbox/dark-v11"

      map.current = new window.mapboxgl.Map({
        container: mapContainer.current,
        style: mapStyle,
        center: [0, 20],
        zoom: 1.5,
        renderWorldCopies: false, // Prevent rendering multiple world copies
      })

      if (window.mapboxgl) {
        map.current.addControl(new window.mapboxgl.NavigationControl(), "top-left")
      }

      map.current.on("load", () => {
        console.log("Map loaded successfully")
        
        // Apply theme-specific colors with proper checks
        try {
          if (theme === "dark") {
            // Dark mode: dark blue water and lighter land
            if (map.current?.getLayer('water')) {
              map.current.setPaintProperty('water', 'fill-color', '#0a193f')
            }
            if (map.current?.getLayer('land')) {
              map.current.setPaintProperty('land', 'fill-color', '#a7a7bdff')
            }
            if (map.current?.getLayer('country-label')) {
              map.current.setPaintProperty('country-label', 'text-color', '#ffffff')
            }
          } else {
            // Light mode: light blue water and white land
            if (map.current?.getLayer('water')) {
              map.current.setPaintProperty('water', 'fill-color', '#3055ff')
            }
            if (map.current?.getLayer('land')) {
              map.current.setPaintProperty('land', 'fill-color', '#f8f9fa')
            }
            if (map.current?.getLayer('country-label')) {
              map.current.setPaintProperty('country-label', 'text-color', '#1a1a1a')
            }
          }
          
          // If there's a landcover layer, adjust it too
          if (map.current?.getLayer('landcover')) {
            const landcoverColor = theme === "dark" ? '#e4e4e7' : '#ffffff'
            map.current.setPaintProperty('landcover', 'fill-color', landcoverColor)
          }
        } catch (error) {
          console.warn('Error applying theme colors to map:', error)
        }
      })
    }

    loadMapbox()

    return () => {
      if (map.current) {
        map.current.remove()
        map.current = null
      }
    }
  }, [])

  // Update map style and colors when theme changes
  useEffect(() => {
    if (!map.current || !map.current.loaded()) return

    const mapStyle = theme === "light" 
      ? "mapbox://styles/mapbox/light-v11" 
      : "mapbox://styles/mapbox/dark-v11"

    // Change the map style
    map.current.setStyle(mapStyle)

    // Wait for the new style to load before applying colors
    map.current.once('style.load', () => {
      try {
        if (theme === "dark") {
          // Dark mode colors
          if (map.current?.getLayer('water')) {
            map.current.setPaintProperty('water', 'fill-color', '#0a193f')
          }
          if (map.current?.getLayer('land')) {
            map.current.setPaintProperty('land', 'fill-color', '#a7a7bdff')
          }
          if (map.current?.getLayer('country-label')) {
            map.current.setPaintProperty('country-label', 'text-color', '#ffffff')
          }
          if (map.current?.getLayer('landcover')) {
            map.current.setPaintProperty('landcover', 'fill-color', '#e4e4e7')
          }
        } else {
          // Light mode colors
          if (map.current?.getLayer('water')) {
            map.current.setPaintProperty('water', 'fill-color', '#c6def1')
          }
          if (map.current?.getLayer('land')) {
            map.current.setPaintProperty('land', 'fill-color', '#f8f9fa')
          }
          if (map.current?.getLayer('country-label')) {
            map.current.setPaintProperty('country-label', 'text-color', '#1a1a1a')
          }
          if (map.current?.getLayer('landcover')) {
            map.current.setPaintProperty('landcover', 'fill-color', '#ffffff')
          }
        }
      } catch (error) {
        console.warn('Error applying theme colors after style change:', error)
      }

      // Re-add route layers after style change
      if (optimalRoutesData) {
        Object.entries(ROUTE_CONFIG).forEach(([routeKey, config]) => {
          const route = optimalRoutesData[routeKey as keyof OptimalRoutesData]
          if (route && route.coordinates && route.coordinates.length > 0) {
            // Reconstruct the route layers
            let geometry
            if (route.geometry) {
              geometry = route.geometry
            } else {
              geometry = {
                type: "LineString",
                coordinates: route.coordinates
              }
            }
            
            const geojson = {
              type: "FeatureCollection",
              features: [{
                type: "Feature",
                geometry: geometry,
                properties: route.metrics
              }]
            }

            const fixedGeojson = geometry.type === "LineString" 
              ? fixAntimeridianWrapping(geojson)
              : geojson
            
            if (!fixedGeojson) return
            
            // Add source and layer
            if (!map.current.getSource(`source-${routeKey}`)) {
              map.current.addSource(`source-${routeKey}`, {
                type: "geojson",
                data: fixedGeojson,
                lineMetrics: true,
              })

              map.current.addLayer({
                id: `route-${routeKey}`,
                type: "line",
                source: `source-${routeKey}`,
                layout: {
                  "line-join": "round",
                  "line-cap": "round",
                  "visibility": visibleRoutes[routeKey] ? "visible" : "none"
                },
                paint: {
                  "line-color": config.color,
                  "line-width": 3,
                  "line-opacity": 0.8,
                },
              })
            }
          }
        })
      }

      // Re-add legacy route if exists
      if (geojsonData && !optimalRoutesData) {
        const fixedGeojson = fixAntimeridianWrapping(geojsonData)
        if (fixedGeojson && !map.current.getSource("shipping-route")) {
          map.current.addSource("shipping-route", {
            type: "geojson",
            data: fixedGeojson,
            lineMetrics: true,
          })

          map.current.addLayer({
            id: "shipping-route-layer",
            type: "line",
            source: "shipping-route",
            layout: {
              "line-join": "round",
              "line-cap": "round",
            },
            paint: {
              "line-color": "#ef4444",
              "line-width": 3,
              "line-opacity": 0.8,
            },
          })
        }
      }
    })
  }, [theme])

  // Toggle route visibility
  const toggleRoute = (routeKey: string) => {
    setVisibleRoutes(prev => ({
      ...prev,
      [routeKey]: !prev[routeKey]
    }))
    
    if (map.current) {
      const layerId = `route-${routeKey}`
      if (map.current.getLayer(layerId)) {
        map.current.setLayoutProperty(
          layerId,
          'visibility',
          visibleRoutes[routeKey] ? 'none' : 'visible'
        )
      }
    }
  }

  // Update map when optimalRoutesData changes
  useEffect(() => {
    if (!map.current || !optimalRoutesData) {
      console.log("WorldMap: No map or optimalRoutesData", { map: !!map.current, optimalRoutesData })
      return
    }

    const updateRoutes = () => {
      console.log("WorldMap: Updating routes with data:", optimalRoutesData)
      
      // Remove all existing route layers and sources
      Object.keys(ROUTE_CONFIG).forEach(routeKey => {
        const layerId = `route-${routeKey}`
        const sourceId = `source-${routeKey}`
        
        if (map.current.getLayer(layerId)) {
          map.current.removeLayer(layerId)
        }
        if (map.current.getSource(sourceId)) {
          map.current.removeSource(sourceId)
        }
      })

      // Calculate bounds for all routes
      if (!window.mapboxgl) return
      
      const bounds = new window.mapboxgl.LngLatBounds()
      let hasRoutes = false

      // Add each optimal route
      Object.entries(ROUTE_CONFIG).forEach(([routeKey, config]) => {
        const route = optimalRoutesData[routeKey as keyof OptimalRoutesData]
        
        console.log(`WorldMap: Processing route ${routeKey}:`, route)
        
        if (route && route.coordinates && route.coordinates.length > 0) {
          hasRoutes = true
          console.log(`WorldMap: Adding route ${routeKey}`, route)
          
          // Use the geometry from the route if available (from API response)
          let geometry
          if (route.geometry) {
            console.log(`WorldMap: Using API geometry for ${routeKey}:`, route.geometry.type)
            geometry = route.geometry
          } else {
            // Fallback: assume LineString
            console.log(`WorldMap: Creating LineString geometry for ${routeKey}`)
            geometry = {
              type: "LineString",
              coordinates: route.coordinates
            }
          }
          
          const geojson = {
            type: "FeatureCollection",
            features: [{
              type: "Feature",
              geometry: geometry,
              properties: route.metrics
            }]
          }

          // Only fix antimeridian for LineString (MultiLineString already handled by API)
          const fixedGeojson = geometry.type === "LineString" 
            ? fixAntimeridianWrapping(geojson)
            : geojson
          
          console.log(`WorldMap: Using ${geometry.type} for ${routeKey}`, fixedGeojson)
          
          // Skip if geojson is invalid
          if (!fixedGeojson) {
            console.log(`WorldMap: Invalid geojson for ${routeKey}`)
            return
          }
          
          // Add source
          map.current.addSource(`source-${routeKey}`, {
            type: "geojson",
            data: fixedGeojson,
            lineMetrics: true,
          })

          // Add layer
          map.current.addLayer({
            id: `route-${routeKey}`,
            type: "line",
            source: `source-${routeKey}`,
            layout: {
              "line-join": "round",
              "line-cap": "round",
              "visibility": visibleRoutes[routeKey] ? "visible" : "none"
            },
            paint: {
              "line-color": config.color,
              "line-width": 3,
              "line-opacity": 0.8,
            },
          })

          // Add click handler for the route
          map.current.on('click', `route-${routeKey}`, () => {
            console.log(`Clicked on ${routeKey}`)
            setSelectedRoute({
              type: routeKey,
              label: config.label,
              metrics: route.metrics,
              routeDetails: routeDetails
            })
          })

          // Change cursor on hover
          map.current.on('mouseenter', `route-${routeKey}`, () => {
            map.current.getCanvas().style.cursor = 'pointer'
          })

          map.current.on('mouseleave', `route-${routeKey}`, () => {
            map.current.getCanvas().style.cursor = ''
          })

          console.log(`WorldMap: Added layer route-${routeKey} with visibility:`, visibleRoutes[routeKey])

          // Extend bounds
          const feature = fixedGeojson.features[0]
          if (feature.geometry.type === 'LineString') {
            (feature.geometry.coordinates as number[][]).forEach((coord) => bounds.extend(coord as [number, number]))
          } else if (feature.geometry.type === 'MultiLineString') {
            (feature.geometry.coordinates as number[][][]).forEach((segment) => {
              segment.forEach((coord) => bounds.extend(coord as [number, number]))
            })
          }
        } else {
          console.log(`WorldMap: Route ${routeKey} has no coordinates or is empty`)
        }
      })

      // Fit map to show all routes
      if (hasRoutes && !bounds.isEmpty()) {
        console.log("WorldMap: Fitting bounds to routes")
        map.current.fitBounds(bounds, { padding: 80 })
      } else {
        console.log("WorldMap: No routes to fit bounds to")
      }
    }

    // Wait for map to be loaded
    if (map.current.loaded()) {
      updateRoutes()
    } else {
      map.current.on("load", updateRoutes)
    }
  }, [optimalRoutesData, visibleRoutes])

  // Update map when geojsonData changes (legacy support)
  useEffect(() => {
    if (!map.current || !geojsonData) return

    const updateRoute = () => {
      // Remove existing source and layer if they exist
      if (map.current.getLayer("shipping-route-layer")) {
        map.current.removeLayer("shipping-route-layer")
      }
      if (map.current.getSource("shipping-route")) {
        map.current.removeSource("shipping-route")
      }

      // Fix antimeridian wrapping before adding to map
      const fixedGeojson = fixAntimeridianWrapping(geojsonData)
      if (!fixedGeojson) return
      console.log("Fixed GeoJSON:", fixedGeojson.features[0].geometry.type)

      // Add new source and layer with lineMetrics enabled for better rendering
      map.current.addSource("shipping-route", {
        type: "geojson",
        data: fixedGeojson,
        lineMetrics: true,
      })

      map.current.addLayer({
        id: "shipping-route-layer",
        type: "line",
        source: "shipping-route",
        layout: {
          "line-join": "round",
          "line-cap": "round",
        },
        paint: {
          "line-color": "#ef4444", // Red color
          "line-width": 3,
          "line-opacity": 0.8,
        },
      })

      // Fit map to route bounds - handle both LineString and MultiLineString
      if (fixedGeojson.features && fixedGeojson.features.length > 0) {
        if (!window.mapboxgl) return
        
        const bounds = new window.mapboxgl.LngLatBounds()
        const feature = fixedGeojson.features[0]
        
        if (feature.geometry.type === 'LineString') {
          (feature.geometry.coordinates as number[][]).forEach((coord) => bounds.extend(coord as [number, number]))
        } else if (feature.geometry.type === 'MultiLineString') {
          (feature.geometry.coordinates as number[][][]).forEach((segment) => {
            segment.forEach((coord) => bounds.extend(coord as [number, number]))
          })
        }
        
        map.current.fitBounds(bounds, { padding: 80 })
      }
    }

    // Wait for map to be loaded
    if (map.current.loaded()) {
      updateRoute()
    } else {
      map.current.on("load", updateRoute)
    }
  }, [geojsonData])

  return (
    <div className="w-full h-full bg-gradient-to-br from-gray-900 via-black to-gray-900 relative overflow-hidden">
      {/* Stars effect for space theme */}
      <div className="absolute inset-0 opacity-50">
        <div className="absolute top-[10%] left-[15%] w-1 h-1 bg-white rounded-full animate-pulse"></div>
        <div className="absolute top-[25%] right-[20%] w-1 h-1 bg-white rounded-full animate-pulse" style={{ animationDelay: '0.5s' }}></div>
        <div className="absolute bottom-[30%] left-[25%] w-0.5 h-0.5 bg-white rounded-full animate-pulse" style={{ animationDelay: '1s' }}></div>
        <div className="absolute top-[60%] right-[35%] w-1 h-1 bg-white rounded-full animate-pulse" style={{ animationDelay: '1.5s' }}></div>
        <div className="absolute bottom-[20%] right-[15%] w-0.5 h-0.5 bg-white rounded-full animate-pulse" style={{ animationDelay: '2s' }}></div>
        <div className="absolute top-[40%] left-[40%] w-0.5 h-0.5 bg-white rounded-full animate-pulse" style={{ animationDelay: '0.3s' }}></div>
        <div className="absolute bottom-[50%] right-[45%] w-1 h-1 bg-white rounded-full animate-pulse" style={{ animationDelay: '1.2s' }}></div>
        <div className="absolute top-[15%] right-[50%] w-0.5 h-0.5 bg-white rounded-full animate-pulse" style={{ animationDelay: '0.8s' }}></div>
      </div>
      
      <div ref={mapContainer} className="w-full h-full absolute inset-0" />
      
      {/* Legend for optimal routes */}
      {optimalRoutesData && (
        <div className="absolute top-4 right-4 space-y-2 z-10 max-w-sm">
          <Card>
            <CardContent className="p-4">
              <h3 className="font-semibold text-lg mb-3">Optimised Routes</h3>
              <div className="space-y-3">
                {Object.entries(ROUTE_CONFIG).map(([routeKey, config]) => {
                  const route = optimalRoutesData[routeKey as keyof OptimalRoutesData]
                  if (!route) return null

                  return (
                    <div key={routeKey} className="border-b pb-3 last:border-b-0">
                      <div className="flex items-center gap-2 mb-2">
                        <Checkbox
                          id={routeKey}
                          checked={visibleRoutes[routeKey]}
                          onCheckedChange={() => toggleRoute(routeKey)}
                        />
                        <Label 
                          htmlFor={routeKey}
                          className="flex items-center gap-2 cursor-pointer font-medium"
                        >
                          <div 
                            className="w-4 h-4 rounded-full" 
                            style={{ backgroundColor: config.color }}
                          />
                          <span className="text-sm">{config.label}</span>
                        </Label>
                      </div>
                      
                      {visibleRoutes[routeKey] && (
                        <div className="ml-6 text-xs text-slate-600 dark:text-slate-400 space-y-1">
                          <div className="flex justify-between">
                            <span>Transport:</span>
                            <span className="font-medium uppercase">{route.metrics.transport_type}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Cost:</span>
                            <span className="font-medium">${route.metrics.cost_usd.toLocaleString()}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Time:</span>
                            <span className="font-medium">{route.metrics.time_hours.toFixed(1)}h</span>
                          </div>
                          <div className="flex justify-between">
                            <span>Risk:</span>
                            <span className="font-medium">{route.metrics.risk_score.toFixed(2)}</span>
                          </div>
                          <div className="flex justify-between">
                            <span>CO2:</span>
                            <span className="font-medium">{route.metrics.co2_kg.toLocaleString()} kg</span>
                          </div>
                        </div>
                      )}
                    </div>
                  )
                })}
              </div>
            </CardContent>
          </Card>
        </div>
      )}
      
      {/* Legacy single route display */}
      {geojsonData && !optimalRoutesData && (
        <div className="absolute top-4 right-4 space-y-2 z-10">
          <Card className="w-64">
            <CardContent className="p-4">
              <div className="flex items-center gap-2 mb-2">
                <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                <span className="text-sm font-medium">Shipping Route</span>
              </div>
              {geojsonData.features?.[0]?.properties && (() => {
                const props = geojsonData.features[0].properties as Record<string, unknown>
                const distance = typeof props.total_distance_km === 'number' ? props.total_distance_km.toFixed(0) : 0
                const waypoints = Array.isArray(props.route_node_ids) ? props.route_node_ids.length : 0
                return (
                  <div className="text-xs text-slate-600 dark:text-slate-400 space-y-1">
                    <div>Distance: {distance} km</div>
                    <div>Waypoints: {waypoints}</div>
                  </div>
                )
              })()}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Route Details Popup */}
      {selectedRoute && (
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-20 w-[400px]">
          <Card className="shadow-2xl border-2 border-blue-500 dark:border-blue-400">
            <CardContent className="p-4">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-semibold text-lg flex items-center gap-2">
                  <div 
                    className="w-4 h-4 rounded-full" 
                    style={{ backgroundColor: ROUTE_CONFIG[selectedRoute.type as keyof typeof ROUTE_CONFIG]?.color }}
                  />
                  {selectedRoute.label}
                </h3>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setSelectedRoute(null)}
                  className="h-6 w-6 p-0"
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>

              {/* Route Information */}
              {selectedRoute.routeDetails && (
                <div className="mb-3 pb-3 border-b border-slate-200 dark:border-slate-700">
                  <h4 className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase mb-2">
                    Shipment Details
                  </h4>
                  <div className="space-y-2">
                    {selectedRoute.routeDetails.sourceCountry && selectedRoute.routeDetails.destinationCountry && (
                      <div className="flex items-center gap-2 text-sm">
                        <Truck className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                        <span className="text-slate-700 dark:text-slate-300">
                          <span className="font-medium">{selectedRoute.routeDetails.sourceCountry}</span>
                          {' → '}
                          <span className="font-medium">{selectedRoute.routeDetails.destinationCountry}</span>
                        </span>
                      </div>
                    )}
                    
                    {selectedRoute.routeDetails.productCode && (
                      <div className="flex items-start gap-2 text-sm">
                        <Package className="h-4 w-4 text-purple-600 dark:text-purple-400 mt-0.5" />
                        <div className="flex-1">
                          <div className="font-medium text-slate-900 dark:text-slate-100">
                            HS Code: {selectedRoute.routeDetails.productCode}
                          </div>
                          {selectedRoute.routeDetails.productDescription && (
                            <div className="text-xs text-slate-600 dark:text-slate-400 line-clamp-2">
                              {selectedRoute.routeDetails.productDescription}
                            </div>
                          )}
                        </div>
                      </div>
                    )}

                    {selectedRoute.routeDetails.tariffAmount !== undefined && selectedRoute.routeDetails.currency && (
                      <div className="flex items-center gap-2 text-sm">
                        <DollarSign className="h-4 w-4 text-green-600 dark:text-green-400" />
                        <span className="text-slate-700 dark:text-slate-300">
                          Tariff Paid: <span className="font-semibold text-green-600 dark:text-green-400">
                            {selectedRoute.routeDetails.currency} {selectedRoute.routeDetails.tariffAmount.toFixed(2)}
                          </span>
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Route Metrics */}
              <div>
                <h4 className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase mb-2">
                  Route Metrics
                </h4>
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-600 dark:text-slate-400 flex items-center gap-2">
                      <Truck className="h-4 w-4" />
                      Transport Mode
                    </span>
                    <span className="font-medium text-slate-900 dark:text-slate-100 uppercase">
                      {selectedRoute.metrics.transport_type}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-600 dark:text-slate-400 flex items-center gap-2">
                      <DollarSign className="h-4 w-4 text-green-600 dark:text-green-400" />
                      Shipping Cost
                    </span>
                    <span className="font-medium text-green-600 dark:text-green-400">
                      ${selectedRoute.metrics.cost_usd.toLocaleString()}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-600 dark:text-slate-400 flex items-center gap-2">
                      <Clock className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                      Transit Time
                    </span>
                    <span className="font-medium text-blue-600 dark:text-blue-400">
                      {selectedRoute.metrics.time_hours.toFixed(1)} hours
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-600 dark:text-slate-400 flex items-center gap-2">
                      <AlertTriangle className="h-4 w-4 text-red-600 dark:text-red-400" />
                      Risk Score
                    </span>
                    <span className="font-medium text-red-600 dark:text-red-400">
                      {selectedRoute.metrics.risk_score.toFixed(2)} / 10
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-sm">
                    <span className="text-slate-600 dark:text-slate-400 flex items-center gap-2">
                      <Leaf className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                      Carbon Footprint
                    </span>
                    <span className="font-medium text-emerald-600 dark:text-emerald-400">
                      {selectedRoute.metrics.co2_kg.toLocaleString()} kg CO₂
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-sm pt-2 border-t border-slate-200 dark:border-slate-700">
                    <span className="text-slate-600 dark:text-slate-400 flex items-center gap-2">
                      Distance
                    </span>
                    <span className="font-medium text-slate-900 dark:text-slate-100">
                      {selectedRoute.metrics.distance_km.toLocaleString()} km
                    </span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
