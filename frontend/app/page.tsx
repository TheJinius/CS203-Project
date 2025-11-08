"use client"

import { useState, useRef, useCallback, useEffect } from "react"
import Sidebar from "@/components/Sidebar"
import WorldMap, { type OptimalRoutesData } from "@/components/WorldMap"
import type { GeoJSONData } from "@/components/WorldMap"
import TopBar from "@/components/TopBar"
import { ProtectedRoute } from "@/components/ProtectedRoute"
import ConditionalChatbot from "@/components/ConditionalChatbot"

export default function TariffCalculatorPage() {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [sidebarWidth, setSidebarWidth] = useState(400)
  const [activeTab, setActiveTab] = useState("calculate")
  const [calculationResult, setCalculationResult] = useState<number | null>(null)
  const [isResizing, setIsResizing] = useState(false)
  const [routeGeojson, setRouteGeojson] = useState<GeoJSONData | null>(null)
  const [optimalRoutesData, setOptimalRoutesData] = useState<OptimalRoutesData | null>(null)
  
  const sidebarRef = useRef<HTMLDivElement>(null)
  const isResizingRef = useRef(false)
  const lastResizeTime = useRef(0)

  const handleRouteCalculated = useCallback((data: Record<string, unknown>) => {
    console.log("🗺️ Routes calculated, updating map with data:", data)
    
    // Check if this is the new optimal routes format (GeoJSON with features array)
    if (data.features && data.metadata) {
      console.log("📊 Transforming optimal routes format")
      // Transform GeoJSON features into our component format
      const transformed = {} as Record<string, unknown>
      
      (data.features as Array<Record<string, unknown>>).forEach((feature: Record<string, unknown>) => {
        const props = feature.properties as Record<string, unknown>
        const geom = feature.geometry as Record<string, unknown>
        const optType = props?.optimization_type as string | undefined
        console.log(`Processing feature with optimization_type: ${optType}`, feature)
        if (optType) {
          transformed[optType] = {
            // Preserve the original geometry (could be LineString or MultiLineString)
            coordinates: geom.coordinates,
            geometry: feature.geometry, // Keep full geometry info
            metrics: {
              distance_km: props.distance_km,
              cost_usd: props.cost_usd,
              time_hours: props.time_hours,
              co2_kg: props.co2_kg,
              risk_score: props.risk_score,
              transport_type: props.transport_type
            },
            optimization: optType.replace('_optimized', '')
          }
        }
      })
      
      console.log("📊 Transformed data:", transformed)
      setOptimalRoutesData(transformed as unknown as OptimalRoutesData)
      setRouteGeojson(null) // Clear legacy format
    } else if (data.features) {
      // Legacy single route format
      console.log("🚢 Using legacy single route format")
      setRouteGeojson(data as unknown as GeoJSONData)
      setOptimalRoutesData(null)
    }
  }, [])

  const startResizing = useCallback(() => {
    setIsResizing(true)
    isResizingRef.current = true
    document.body.style.userSelect = 'none'
    document.body.style.cursor = 'col-resize'
  }, [])

  const stopResizing = useCallback(() => {
    setIsResizing(false)
    isResizingRef.current = false
    document.body.style.userSelect = ''
    document.body.style.cursor = ''
  }, [])

  const resize = useCallback((mouseMoveEvent: MouseEvent) => {
    if (isResizingRef.current && sidebarRef.current) {
      const now = Date.now()
      // Throttle to 60fps (16.67ms)
      if (now - lastResizeTime.current < 16) return
      lastResizeTime.current = now
      
      requestAnimationFrame(() => {
        const newWidth = mouseMoveEvent.clientX
        // Better width constraints for different screen sizes
        const minWidth = 320
        const maxWidth = Math.min(800, window.innerWidth * 0.6)
        
        if (newWidth >= minWidth && newWidth <= maxWidth) {
          setSidebarWidth(newWidth)
        }
      })
    }
  }, [])

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      e.preventDefault()
      resize(e)
    }
    const handleMouseUp = (e: MouseEvent) => {
      e.preventDefault()
      stopResizing()
    }

    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
      document.addEventListener('mouseleave', handleMouseUp)
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
      document.removeEventListener('mouseleave', handleMouseUp)
    }
  }, [isResizing, resize, stopResizing])

  return (
    <ProtectedRoute>
      <div className="flex h-screen bg-slate-100 dark:bg-slate-950 text-slate-900 dark:text-slate-100 overflow-hidden">
        {/* Sidebar Container */}
        {sidebarOpen && (
          <div
            ref={sidebarRef}
            className={`relative flex-shrink-0 ${
              isResizing ? 'select-none transition-none' : 'transition-all duration-200 ease-in-out'
            }`}
            style={{ 
              width: `${sidebarWidth}px`,
              minWidth: `${sidebarWidth}px`,
              maxWidth: `${sidebarWidth}px`
            }}
          >
            <Sidebar
              isOpen={sidebarOpen}
              activeTab={activeTab}
              onTabChange={setActiveTab}
              onClose={() => setSidebarOpen(false)}
              calculationResult={calculationResult}
              onCalculationResult={setCalculationResult}
              onRouteCalculated={handleRouteCalculated}
              width={sidebarWidth}
            />
            
            {/* Improved Resize Handle */}
            <div
              className={`absolute top-0 right-0 h-full w-1 bg-transparent hover:bg-blue-200 dark:hover:bg-blue-800 cursor-col-resize group transition-all duration-200 z-10 ${
                isResizing ? 'bg-blue-300 dark:bg-blue-700' : ''
              }`}
              onMouseDown={startResizing}
              title="Drag to resize sidebar"
            >
              {/* Visual grip indicator */}
              <div className={`absolute top-1/2 right-0 transform translate-x-1/2 -translate-y-1/2 w-3 h-12 bg-slate-300 dark:bg-slate-600 group-hover:bg-blue-400 dark:group-hover:bg-blue-500 rounded-l-full flex items-center justify-center transition-all duration-200 ${
                isResizing ? 'bg-blue-500 dark:bg-blue-400 scale-110' : ''
              }`}>
                {/* Grip dots */}
                <div className="space-y-1">
                  <div className="w-1 h-1 bg-white dark:bg-slate-200 rounded-full opacity-70"></div>
                  <div className="w-1 h-1 bg-white dark:bg-slate-200 rounded-full opacity-70"></div>
                  <div className="w-1 h-1 bg-white dark:bg-slate-200 rounded-full opacity-70"></div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Main Content Area */}
        <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
          <TopBar 
            sidebarOpen={sidebarOpen}
            onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}
          />

          <div className="flex-1 overflow-hidden bg-white dark:bg-slate-900 relative">
            <WorldMap geojsonData={routeGeojson} optimalRoutesData={optimalRoutesData} />
            
            {/* Loading overlay when resizing */}
            {isResizing && (
              <div className="absolute inset-0 bg-black/5 dark:bg-white/5 pointer-events-none z-20" />
            )}
          </div>
        </div>
      </div>
      
      {/* Chatbot */}
      <ConditionalChatbot />
    </ProtectedRoute>
  )
}