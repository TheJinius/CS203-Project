"use client"

import { useState, useRef, useCallback, useEffect } from "react"
import Sidebar from "@/components/Sidebar"
import WorldMap from "@/components/WorldMap"
import TopBar from "@/components/TopBar"
import { ProtectedRoute } from "@/components/ProtectedRoute"

export default function TariffCalculatorPage() {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [sidebarWidth, setSidebarWidth] = useState(400) // Add this
  const [activeTab, setActiveTab] = useState("calculate")
  const [calculationResult, setCalculationResult] = useState<number | null>(null)
  const [isResizing, setIsResizing] = useState(false) // Add this
  
  const sidebarRef = useRef<HTMLDivElement>(null) // Add this
  const isResizingRef = useRef(false) // Add this

  // Add these functions
  const startResizing = useCallback(() => {
    setIsResizing(true)
    isResizingRef.current = true
  }, [])

  const stopResizing = useCallback(() => {
    setIsResizing(false)
    isResizingRef.current = false
  }, [])

  const resize = useCallback((mouseMoveEvent: MouseEvent) => {
    if (isResizingRef.current && sidebarRef.current) {
      const newWidth = mouseMoveEvent.clientX
      if (newWidth >= 300 && newWidth <= 600) {
        setSidebarWidth(newWidth)
      }
    }
  }, [])

  // Add this useEffect
  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => resize(e)
    const handleMouseUp = () => stopResizing()

    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
    }
  }, [isResizing, resize, stopResizing])

  return (
    <ProtectedRoute>
      <div className="flex h-screen bg-background text-foreground dark:bg-gray-900 dark:text-white">
        {/* REPLACE your Sidebar section with this: */}
        <div
          ref={sidebarRef}
          className={`relative transition-all duration-300 ${
            sidebarOpen ? 'block' : 'hidden'
          }`}
          style={{ width: sidebarOpen ? `${sidebarWidth}px` : '0px' }}
        >
          <Sidebar
            isOpen={sidebarOpen}
            activeTab={activeTab}
            onTabChange={setActiveTab}
            onClose={() => setSidebarOpen(false)}
            calculationResult={calculationResult}
            onCalculationResult={setCalculationResult}
            width={sidebarWidth}  // Add this prop
          />
          
          {/* Add this resize handle */}
          <div
            className="absolute top-0 right-0 w-1 h-full bg-border hover:bg-primary cursor-col-resize group transition-colors"
            onMouseDown={startResizing}
          >
            <div className="absolute top-1/2 right-0 transform translate-x-1/2 -translate-y-1/2 w-3 h-8 bg-border group-hover:bg-primary rounded-sm flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
              <div className="w-0.5 h-4 bg-background dark:bg-gray-900"></div>
            </div>
          </div>
        </div>

        <div className="flex-1 flex flex-col min-w-0">
          <TopBar 
            sidebarOpen={sidebarOpen}
            onToggleSidebar={() => setSidebarOpen(!sidebarOpen)}  // Fix this
          />

          <div className="flex-1 overflow-hidden">
            <WorldMap />
          </div>
        </div>
      </div>
    </ProtectedRoute>
  )
}