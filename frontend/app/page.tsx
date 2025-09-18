"use client"

import { useState } from "react"
import { Menu } from "lucide-react"
import { Button } from "@/components/ui/button"
import Sidebar from "@/components/Sidebar"
import WorldMap from "@/components/WorldMap"

export default function TariffCalculatorPage() {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const [activeTab, setActiveTab] = useState("calculate")
  const [calculationResult, setCalculationResult] = useState<number | null>(null)

  return (
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
        <header className="h-16 border-b border-border bg-card flex items-center px-4">
          {!sidebarOpen && (
            <Button variant="ghost" size="sm" onClick={() => setSidebarOpen(true)} className="mr-4">
              <Menu className="h-4 w-4" />
            </Button>
          )}
          <h2 className="text-lg font-semibold text-card-foreground">Global Trade Map</h2>
        </header>
        
        <WorldMap />
      </div>
    </div>
  )
}
