"use client"

import { useState } from "react"
import { X, Calculator, Package, MapPin, FileText, TrendingUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import Image from "next/image"
import CalculateTab from "./tabs/CalculateTab"
import ProductsTab from "./tabs/ProductsTab"
import CountriesTab from "./tabs/CountriesTab"
import TariffsTab from "./tabs/TariffsTab"
import ResultsTab from "./tabs/ResultsTab"

interface SidebarProps {
  isOpen: boolean
  activeTab: string
  onTabChange: (tab: string) => void
  onClose: () => void
  calculationResult: number | null
  onCalculationResult: (result: number | null) => void
}

export default function Sidebar({ 
  isOpen, 
  activeTab, 
  onTabChange, 
  onClose, 
  calculationResult, 
  onCalculationResult 
}: SidebarProps) {
  const sidebarItems = [
    { id: "calculate", label: "Calculate Tariff", icon: Calculator },
    { id: "products", label: "Products", icon: Package },
    { id: "countries", label: "Countries", icon: MapPin },
    { id: "tariffs", label: "Tariffs", icon: FileText },
    { id: "results", label: "Results", icon: TrendingUp },
  ]

  return (
    <div
      className={`${isOpen ? "w-80" : "w-0"} transition-all duration-300 overflow-hidden bg-sidebar border-r border-sidebar-border`}
    >
      <div className="p-4">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-2">
            <Image
              src="/TOP light.png"
              alt="Tariff Calculator Logo"
              width={160}  
              height={40}
              priority
            />
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            className="text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Navigation */}
        <nav className="space-y-2 mb-6">
          {sidebarItems.map((item) => {
            const Icon = item.icon
            return (
              <Button
                key={item.id}
                variant={activeTab === item.id ? "default" : "ghost"}
                className={`w-full justify-start gap-2 ${
                  activeTab === item.id
                    ? "bg-sidebar-primary text-sidebar-primary-foreground"
                    : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                }`}
                onClick={() => onTabChange(item.id)}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Button>
            )
          })}
        </nav>

        <Separator className="mb-6" />

        {/* Tab Content */}
        <div className="space-y-4">
          {activeTab === "calculate" && (
            <CalculateTab 
              onCalculationResult={onCalculationResult}
            />
          )}
          {activeTab === "products" && <ProductsTab />}
          {activeTab === "countries" && <CountriesTab />}
          {activeTab === "tariffs" && <TariffsTab />}
          {activeTab === "results" && (
            <ResultsTab calculationResult={calculationResult} />
          )}
        </div>
      </div>
    </div>
  )
}