"use client"

import { useState,useEffect } from "react"
import { X, Calculator, Package, MapPin, FileText, TrendingUp, Edit } from "lucide-react"
import { Button } from "@/components/ui/button"
// import { Separator } from "@/components/ui/separator"
import Image from "next/image"
import CalculateTab from "./tabs/CalculateTab"
import ProductsTab from "./tabs/ProductsTab"
import CountriesTab from "./tabs/CountriesTab"
import TariffsTab from "./tabs/TariffsTab"
import ResultsTab from "./tabs/ResultsTab"
import EditTariffTab from "./tabs/EditTariffTab"
import { useAuth } from "../contexts/AuthContext"
import { useTheme } from "../contexts/ThemeContext"

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
  width
}: SidebarProps) {
  const { isAdmin } = useAuth();
  const { theme } = useTheme();

  const [currency, setCurrency] = useState<string>("USD")

  // If a non-admin somehow lands on "edit", send them to a safe tab
  useEffect(() => {
    if (activeTab === "edit" && !isAdmin()) {
      onTabChange("calculate")
    }
  }, [activeTab, isAdmin, onTabChange])

  const tabs = [
    { id: "calculate", name: "Calculate", icon: Calculator },
    { id: "products", name: "Products", icon: Package },
    { id: "countries", name: "Countries", icon: MapPin },
    { id: "tariffs", name: "Tariffs", icon: FileText },
    { id: "results", name: "Results", icon: TrendingUp },
    ...(isAdmin() ? [{ id: "edit", name: "Edit Tariff", icon: Edit }] : []),
  ]

  if (!isOpen) return null

  return (
    <div 
      className="h-full bg-slate-50 dark:bg-slate-900 border-r border-slate-200 dark:border-slate-700 flex flex-col shadow-lg overflow-hidden"
      style={{ 
        width: `${width}px`,
        minWidth: `${width}px`,
        maxWidth: `${width}px`
      }}
    >
      {/* Header Section */}
      <div className="p-4 border-b border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Image
              src={theme === 'dark' ? '/TOP dark.png' : '/TOP light.png'}
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
            className="text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 hover:text-slate-900 dark:hover:text-slate-100"
          >
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Navigation Tabs */}
        <nav className="space-y-1">
          {tabs.map((item) => {
            const Icon = item.icon
            return (
              <Button
                key={item.id}
                variant={activeTab === item.id ? "default" : "ghost"}
                className={`w-full justify-start gap-2 text-sm transition-all duration-200 ${
                  activeTab === item.id
                    ? "bg-blue-600 hover:bg-blue-700 text-white dark:bg-blue-600 dark:hover:bg-blue-700"
                    : "text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 hover:text-slate-900 dark:hover:text-slate-100"
                }`}
                onClick={() => {
                  // Extra guard: ignore clicks to "edit" if not admin
                  if (item.id === "edit" && !isAdmin()) return
                  onTabChange(item.id)
                }}
              >
                <Icon className="h-4 w-4 flex-shrink-0" />
                <span className={`truncate ${width < 350 ? 'text-xs' : 'text-sm'}`}>
                  {item.name}
                </span>
              </Button>
            )
          })}
        </nav>
      </div>

      {/* Tab Content Container with proper overflow handling */}
      <div className="flex-1 overflow-hidden flex flex-col">
        <div className="flex-1 overflow-y-auto p-6">
          {/* Calculate Tab - Full height container */}
          {activeTab === "calculate" && (
            <div className="h-full">
              <CalculateTab 
                onCalculationResult={onCalculationResult}
                currency={currency}
                onCurrencyChange={setCurrency}
              />
            </div>
          )}
          
          {/* Other Tabs - Standard padding */}
          {activeTab === "products" && <ProductsTab />}
          {activeTab === "countries" && <CountriesTab />}
          {activeTab === "tariffs" && <TariffsTab />}
          {activeTab === "results" && <ResultsTab calculationResult={calculationResult} currency={currency} />}
          {/* Access control: only render edit tab for admins */}
          {activeTab === "edit" && isAdmin() && <EditTariffTab />}
        </div>
      </div>

      {/* Results Footer - Fixed at bottom */}
      {calculationResult !== null && (
        <div className="border-t border-slate-200 dark:border-slate-700 p-4 bg-slate-100 dark:bg-slate-800">
          <div className="bg-white dark:bg-slate-700 rounded-lg p-3 border border-slate-200 dark:border-slate-600">
            <h3 className="font-semibold text-slate-800 dark:text-slate-200 mb-1 text-sm">
              Latest Result
            </h3>
            <p className="text-lg font-bold text-blue-600 dark:text-blue-400">
              {currency} {calculationResult.toFixed(2)} 
            </p>
          </div>
        </div>
      )}
    </div>
  )
}