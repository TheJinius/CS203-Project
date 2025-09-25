"use client"

import { useEffect } from "react"
import { X, Calculator, Package, MapPin, FileText, TrendingUp, Edit } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import Image from "next/image"
import CalculateTab from "./tabs/CalculateTab"
import ProductsTab from "./tabs/ProductsTab"
import CountriesTab from "./tabs/CountriesTab"
import TariffsTab from "./tabs/TariffsTab"
import ResultsTab from "./tabs/ResultsTab"
import EditTariffTab from "./tabs/EditTariffTab"
import { useAuth } from "../contexts/AuthContext"

interface SidebarProps {
  isOpen: boolean
  activeTab: string
  onTabChange: (tab: string) => void
  onClose: () => void
  calculationResult: number | null
  onCalculationResult: (result: number | null) => void
  width: number 
}

export default function Sidebar({ 
  isOpen, 
  activeTab, 
  onTabChange, 
  onClose, 
  calculationResult, 
  onCalculationResult,
  width // Add this parameter
}: SidebarProps) {
  const { isAdmin } = useAuth();

  // If a non-admin somehow lands on "edit", send them to a safe tab
  useEffect(() => {
    if (activeTab === "edit" && !isAdmin()) {
      onTabChange("calculate")
    }
  }, [activeTab, isAdmin, onTabChange])

  const sidebarItems = [
    { id: "calculate", label: "Calculate Tariff", icon: Calculator },
    { id: "products", label: "Products", icon: Package },
    { id: "countries", label: "Countries", icon: MapPin },
    { id: "tariffs", label: "Tariffs", icon: FileText },
    { id: "results", label: "Results", icon: TrendingUp },
    // Only include "Edit tariffs" if user is admin
    ...(isAdmin() ? [{ id: "edit", label: "Edit Tariffs", icon: Edit }] : []),
  ]

  if (!isOpen) return null

  return (
    <div
      className="h-full bg-sidebar border-r border-sidebar-border flex flex-col shadow-lg dark:bg-gray-800 dark:border-gray-700"
      style={{ width: `${width}px` }} // Use the dynamic width!
    >
      {/* Header Section */}
      <div className="p-4 border-b border-sidebar-border dark:border-gray-700">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Image
              src="/TOP light.png"
              alt="Tariff Calculator Logo"
              width={Math.min(160, width - 80)} // Responsive logo size
              height={40}
              priority
              className="object-contain"
            />
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={onClose}
            className="text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground dark:text-white dark:hover:bg-gray-700"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Navigation Tabs */}
        <nav className="space-y-1">
          {sidebarItems.map((item) => {
            const Icon = item.icon
            return (
              <Button
                key={item.id}
                variant={activeTab === item.id ? "default" : "ghost"}
                className={`w-full justify-start gap-2 text-sm ${
                  activeTab === item.id
                    ? "bg-sidebar-primary text-sidebar-primary-foreground dark:bg-blue-600 dark:text-white"
                    : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground dark:text-gray-300 dark:hover:bg-gray-700"
                }`}
                onClick={() => {
                  // Extra guard: ignore clicks to "edit" if not admin
                  if (item.id === "edit" && !isAdmin()) return
                  onTabChange(item.id)
                }}
              >
                <Icon className="h-4 w-4 flex-shrink-0" />
                <span className={`truncate ${width < 350 ? 'text-xs' : 'text-sm'}`}>
                  {item.label}
                </span>
              </Button>
            )
          })}
        </nav>
      </div>

      {/* Tab Content Area - Scrollable */}
      <div className="flex-1 overflow-y-auto">
        <div className="p-4">
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
          {/* Access control: only render edit tab for admins */}
          {activeTab === "edit" && isAdmin() && <EditTariffTab />}
        </div>
      </div>

      {/* Results Footer - Fixed at bottom */}
      {calculationResult !== null && (
        <div className="border-t border-sidebar-border dark:border-gray-700 p-4 bg-sidebar-accent/50 dark:bg-gray-800">
          <div className="bg-sidebar dark:bg-gray-700 rounded-lg p-3">
            <h3 className="font-semibold text-sidebar-foreground dark:text-white mb-1 text-sm">
              Latest Result
            </h3>
            <p className="text-lg font-bold text-primary dark:text-blue-400">
              ${calculationResult.toFixed(2)}
            </p>
          </div>
        </div>
      )}
    </div>
  )
}