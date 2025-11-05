import { useEffect, useRef } from "react"
import { Card, CardContent } from "@/components/ui/card"

interface WorldMapProps {
  geojsonData?: any
}

// More robust antimeridian fix using proper geodesic logic
function fixAntimeridianWrapping(geojsonData: any) {
  if (!geojsonData?.features?.[0]?.geometry?.coordinates) {
    return geojsonData
  }

  const feature = geojsonData.features[0]
  const coords = feature.geometry.coordinates
  
  // Check if it's already a MultiLineString
  if (feature.geometry.type === 'MultiLineString') {
    return geojsonData
  }

  const segments: number[][][] = []
  let currentSegment: number[][] = []
  
  for (let i = 0; i < coords.length; i++) {
    const currentPoint = coords[i]
    
    if (i === 0) {
      currentSegment.push(currentPoint)
      continue
    }
    
    const prevPoint = coords[i - 1]
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

export default function WorldMap({ geojsonData }: WorldMapProps) {
  const mapContainer = useRef<HTMLDivElement>(null)
  const map = useRef<any>(null)

  useEffect(() => {
    if (map.current) return

    const loadMapbox = async () => {
      if (!document.querySelector('link[href*="mapbox-gl"]')) {
        const link = document.createElement("link")
        link.href = "https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css"
        link.rel = "stylesheet"
        document.head.appendChild(link)
      }

      if (!(window as any).mapboxgl) {
        const script = document.createElement("script")
        script.src = "https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js"
        script.onload = initializeMap
        document.head.appendChild(script)
      } else {
        initializeMap()
      }
    }

    const initializeMap = () => {
      if (!(window as any).mapboxgl || !mapContainer.current) return

      ;(window as any).mapboxgl.accessToken = process.env.NEXT_PUBLIC_MAPBOX_TOKEN || ""

      map.current = new (window as any).mapboxgl.Map({
        container: mapContainer.current,
        style: "mapbox://styles/mapbox/light-v11",
        center: [0, 20],
        zoom: 1.5,
        renderWorldCopies: false, // Prevent rendering multiple world copies
      })

      map.current.addControl(new (window as any).mapboxgl.NavigationControl(), "top-left")

      map.current.on("load", () => {
        console.log("Map loaded successfully")
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

  // Update map when geojsonData changes
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
        const bounds = new (window as any).mapboxgl.LngLatBounds()
        const feature = fixedGeojson.features[0]
        
        if (feature.geometry.type === 'LineString') {
          feature.geometry.coordinates.forEach((coord: any) => bounds.extend(coord))
        } else if (feature.geometry.type === 'MultiLineString') {
          feature.geometry.coordinates.forEach((segment: any) => {
            segment.forEach((coord: any) => bounds.extend(coord))
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
    <div className="w-full h-full bg-gradient-to-br from-blue-50 to-blue-100 relative">
      <div ref={mapContainer} className="w-full h-full absolute inset-0" />
      {geojsonData && (
        <div className="absolute top-4 right-4 space-y-2 z-10">
          <Card className="w-64">
            <CardContent className="p-4">
              <div className="flex items-center gap-2 mb-2">
                <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                <span className="text-sm font-medium">Shipping Route</span>
              </div>
              {geojsonData.features?.[0]?.properties && (
                <div className="text-xs text-slate-600 dark:text-slate-400 space-y-1">
                  <div>Distance: {geojsonData.features[0].properties.total_distance_km?.toFixed(0)} km</div>
                  <div>Waypoints: {geojsonData.features[0].properties.route_node_ids?.length || 0}</div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
