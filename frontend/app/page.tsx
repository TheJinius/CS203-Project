"use client"

import { useState } from "react"
import Sidebar from "@/components/Sidebar"
import WorldMap from "@/components/WorldMap"
import TopBar from "@/components/TopBar"
import { ProtectedRoute } from "@/components/ProtectedRoute"

export default function TariffCalculatorPage() {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [activeTab, setActiveTab] = useState("calculate")
  const [calculationResult, setCalculationResult] = useState<number | null>(null)

  return (
    <ProtectedRoute>
      <div className="flex h-screen bg-background">
        <Sidebar
          isOpen={sidebarOpen}
          activeTab={activeTab}
          onTabChange={setActiveTab}
          onClose={() => setSidebarOpen(false)}
          calculationResult={calculationResult}
          onCalculationResult={setCalculationResult}
        />

        <div className="flex-1 flex flex-col">
          <TopBar 
            sidebarOpen={sidebarOpen}
            onToggleSidebar={() => setSidebarOpen(true)}
          />

          <WorldMap />
        </div>
      </div>
    </ProtectedRoute>
  )
}