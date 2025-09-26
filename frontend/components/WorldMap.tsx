import { useEffect, useRef } from "react"
import { Card, CardContent } from "@/components/ui/card"

export default function WorldMap() {
  const mapContainer = useRef(null)
  const map = useRef(null)

  useEffect(() => {
    // Only initialize map once
    if (map.current) return

    // Load Mapbox GL JS script and CSS
    const loadMapbox = async () => {
      // Load CSS
      if (!document.querySelector('link[href*="mapbox-gl"]')) {
        const link = document.createElement('link')
        link.href = 'https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css'
        link.rel = 'stylesheet'
        document.head.appendChild(link)
      }

      // Load JavaScript
      if (!window.mapboxgl) {
        const script = document.createElement('script')
        script.src = 'https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js'
        script.onload = initializeMap
        document.head.appendChild(script)
      } else {
        initializeMap()
      }
    }

    const initializeMap = () => {
      if (!window.mapboxgl || !mapContainer.current) return

      // You'll need to replace this with your actual Mapbox access token
      window.mapboxgl.accessToken = process.env.NEXT_PUBLIC_MAPBOX_TOKEN

      map.current = new window.mapboxgl.Map({
        container: mapContainer.current,
        style: 'mapbox://styles/mapbox/light-v11',
        center: [0, 20], // [longitude, latitude]
        zoom: 1.5
      })

      // Add navigation control
      map.current.addControl(new window.mapboxgl.NavigationControl(), 'top-left')

      // Optional: Add some basic interaction handlers
      map.current.on('load', () => {
        console.log('Map loaded successfully')
      })
    }

    loadMapbox()

    // Cleanup function
    return () => {
      if (map.current) {
        map.current.remove()
        map.current = null
      }
    }
  }, [])

  return (
    <div className="w-full h-full bg-gradient-to-br from-blue-50 to-blue-100 relative">
      {/* Map Container */}
      <div 
        ref={mapContainer}
        className="w-full h-full absolute inset-0"
      />
      
      {/* Map overlay info */}
      <div className="absolute top-4 right-4 space-y-2 z-10">
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
    </div>
  )
}