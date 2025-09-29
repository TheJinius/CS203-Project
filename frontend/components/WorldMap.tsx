import { useEffect, useRef, useState } from "react"
import { Card, CardContent } from "@/components/ui/card"

export default function WorldMap() {
  const mapContainer = useRef(null)
  const map = useRef(null)
  const [selectedCountry, setSelectedCountry] = useState("Singapore")
  const [routeInfo, setRouteInfo] = useState(null)

  // Example destinations (you can make this dynamic)
  const destinations = {
    "New York": [-74.006, 40.7128],
    "London": [-0.1276, 51.5074],
    "Tokyo": [139.6917, 35.6895],
    "Sydney": [151.2093, -33.8688]
  }

  useEffect(() => {
    if (map.current) return

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

      window.mapboxgl.accessToken = process.env.NEXT_PUBLIC_MAPBOX_TOKEN

      map.current = new window.mapboxgl.Map({
        container: mapContainer.current,
        style: 'mapbox://styles/mapbox/light-v11',
        center: [103.827156, 1.261068],
        zoom: 2 // Zoom out to see routes better
      })

      map.current.addControl(new window.mapboxgl.NavigationControl(), 'top-left')

      // Singapore marker
      new window.mapboxgl.Marker({ color: '#22c55e' })
        .setLngLat([103.827156, 1.261068])
        .setPopup(new window.mapboxgl.Popup().setHTML('<h3>Singapore</h3><p>Origin</p>'))
        .addTo(map.current)

      map.current.on('load', () => {
        console.log('Map loaded successfully')
        
        // Add route source (empty initially)
        map.current.addSource('route', {
          type: 'geojson',
          data: {
            type: 'Feature',
            properties: {},
            geometry: {
              type: 'LineString',
              coordinates: []
            }
          }
        })

        // Add route layer
        map.current.addLayer({
          id: 'route',
          type: 'line',
          source: 'route',
          layout: {
            'line-join': 'round',
            'line-cap': 'round'
          },
          paint: {
            'line-color': '#3b82f6', // Blue color
            'line-width': 4,
            'line-opacity': 0.8
          }
        })
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

  // Function to create straight line route (great circle)
  const getRoute = async (destination) => {
    const start = [103.827156, 1.261068] // Singapore
    const end = destinations[destination]

    try {
      // Create a straight line between two points
      const route = {
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'LineString',
          coordinates: [start, end]
        }
      }

      // Update route on map
      map.current.getSource('route').setData(route)

      // Clear existing destination markers first
      const existingMarkers = document.querySelectorAll('.mapboxgl-marker')
      existingMarkers.forEach(marker => {
        if (marker.style.backgroundColor === 'rgb(239, 68, 68)') { // Red markers
          marker.remove()
        }
      })

      // Add destination marker
      new window.mapboxgl.Marker({ color: '#ef4444' })
        .setLngLat(end)
        .setPopup(new window.mapboxgl.Popup().setHTML(`<h3>${destination}</h3><p>Destination</p>`))
        .addTo(map.current)

      // Fit map to show entire route
      const bounds = new window.mapboxgl.LngLatBounds()
      bounds.extend(start)
      bounds.extend(end)
      map.current.fitBounds(bounds, { padding: 50 })

      // Calculate approximate distance (great circle distance)
      const distance = calculateDistance(start[1], start[0], end[1], end[0])
      const flightTime = Math.round(distance / 900) // Approximate flight speed 900 km/h

      // Update route info
      setRouteInfo({
        destination,
        distance: distance.toFixed(0),
        duration: flightTime
      })

      const response = await fetch(
        `https://api.mapbox.com/directions/v5/mapbox/driving/${start[0]},${start[1]};${end[0]},${end[1]}?steps=true&geometries=geojson&access_token=${process.env.NEXT_PUBLIC_MAPBOX_TOKEN}`
      )
    } catch (error) {
      console.error('Error creating route:', error)
    }
  }

  // Helper function to calculate great circle distance
  const calculateDistance = (lat1, lon1, lat2, lon2) => {
    const R = 6371 // Earth's radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180
    const dLon = (lon2 - lon1) * Math.PI / 180
    const a = 
      Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
      Math.sin(dLon/2) * Math.sin(dLon/2)
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
    return R * c
  }

  // Clear route function
  const clearRoute = () => {
    if (map.current && map.current.getSource('route')) {
      map.current.getSource('route').setData({
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'LineString',
          coordinates: []
        }
      })
      setRouteInfo(null)
      
      // Reset map view to Singapore
      map.current.flyTo({
        center: [103.827156, 1.261068],
        zoom: 2
      })
    }
  }

  return (
    <div className="w-full h-full bg-gradient-to-br from-blue-50 to-blue-100 relative">
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
              <span className="text-sm">Origin: {selectedCountry}</span>
            </div>
            <div className="text-xs text-gray-600 mb-3">
              Longitude: 103.827156, Latitude: 1.261068
            </div>
            
            {/* Route Controls */}
            <div className="space-y-2">
              <p className="text-sm font-medium">Get Route To:</p>
              <div className="grid grid-cols-2 gap-1">
                {Object.keys(destinations).map(dest => (
                  <button
                    key={dest}
                    onClick={() => getRoute(dest)}
                    className="text-xs bg-blue-500 text-white px-2 py-1 rounded hover:bg-blue-600"
                  >
                    {dest}
                  </button>
                ))}
              </div>
              
              {routeInfo && (
                <div className="mt-2 p-2 bg-blue-50 rounded text-xs">
                  <p><strong>To:</strong> {routeInfo.destination}</p>
                  <p><strong>Distance:</strong> ~{routeInfo.distance} km</p>
                  <p><strong>Est. Flight Time:</strong> ~{routeInfo.duration} hours</p>
                </div>
              )}
              
              <button
                onClick={clearRoute}
                className="w-full text-xs bg-gray-500 text-white px-2 py-1 rounded hover:bg-gray-600"
              >
                Clear Route
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}