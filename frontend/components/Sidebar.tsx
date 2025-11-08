"use client"

import { useState, useEffect } from "react"
import { X, Calculator, TrendingUp, FolderUp, GitCompare } from "lucide-react"
import { Button } from "@/components/ui/button"
import Image from "next/image"
import { useRouter } from "next/navigation"
import CalculateTab from "./tabs/CalculateTab"
import ResultsTab from "./tabs/ResultsTab"
import ManageRAGChatbotDocumentsTab from "./tabs/ManageRAGChatbotDocumentsTab"
import { useAuth } from "../contexts/AuthContext"
import { useTheme } from "../contexts/ThemeContext"

export interface CalculationHistory {
  id: string
  timestamp: Date
  sourceCountry: string
  destinationCountry: string
  productCode: string
  productDescription: string
  tariffAmount: number
  currency: string
  tariffId: number
  dutyType?: string
  rate?: number
  year: string
}

interface SidebarProps {
  isOpen: boolean
  activeTab: string
  onTabChange: (tab: string) => void
  onClose: () => void
  calculationResult: number | null
  onCalculationResult: (result: number | null) => void
  onRouteCalculated?: (geojson: Record<string, unknown>) => void
  width: number 
}

export default function Sidebar({ 
  isOpen, 
  activeTab, 
  onTabChange, 
  onClose, 
  calculationResult, 
  onCalculationResult,
  onRouteCalculated,
  width
}: SidebarProps) {
  const { isAdmin } = useAuth();
  const { theme } = useTheme();
  const router = useRouter();

  const [currency, setCurrency] = useState<string>("USD")
  const [calculationHistory, setCalculationHistory] = useState<CalculationHistory[]>([])

  // Load calculation history from localStorage
  useEffect(() => {
    const stored = localStorage.getItem('calculationHistory')
    if (stored) {
      try {
        const parsed = JSON.parse(stored)
        setCalculationHistory(parsed)
      } catch (e) {
        console.error('Failed to load calculation history', e)
      }
    }
  }, [])

  // Save calculation history to localStorage whenever it changes
  useEffect(() => {
    if (calculationHistory.length > 0) {
      localStorage.setItem('calculationHistory', JSON.stringify(calculationHistory))
    }
  }, [calculationHistory])

  const handleSaveCalculation = (calculation: CalculationHistory) => {
    setCalculationHistory(prev => [calculation, ...prev])
  }

  // Build sidebar items dynamically based on user role
  const sidebarItems = [
    // Admin-only: Manage Chatbot Documents tab
    ...(isAdmin() ? [{ id: "manage-docs", label: "Manage Chatbot Documents", icon: FolderUp }] : []),
    { id: "calculate", label: "Calculate Tariff", icon: Calculator },
    { id: "results", label: "Results", icon: TrendingUp },
    { id: "compare", label: "Compare Tariffs", icon: GitCompare },
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
          {sidebarItems.map((item) => {
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
                  if (item.id === "compare") {
                    router.push('/compare')
                  } else {
                    onTabChange(item.id)
                  }
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

      {/* Tab Content Container with proper overflow handling */}
      <div className="flex-1 overflow-hidden flex flex-col">
        <div className="flex-1 overflow-y-auto overflow-x-hidden">
          {/* Manage Documents Tab - Admin only */}
          {activeTab === "manage-docs" && (
            <div className="h-full">
              <ManageRAGChatbotDocumentsTab />
            </div>
          )}

          {/* Calculate Tab - Full height container */}
          {activeTab === "calculate" && (
            <div className="h-full">
              <CalculateTab 
                onCalculationResult={onCalculationResult}
                onRouteCalculated={onRouteCalculated}
                currency={currency}
                onCurrencyChange={setCurrency}
                onSaveCalculation={handleSaveCalculation}
              />
            </div>
          )}
          
          {/* Other Tabs - Standard padding */}
          {/* {activeTab === "products" && (
            <div className="p-4">
              <ProductsTab />
            </div>
          )}
          
          {activeTab === "countries" && (
            <div className="p-4">
              <CountriesTab />
            </div>
          )}
          
          {activeTab === "tariffs" && (
            <div className="p-4">
              <TariffsTab />
            </div>
          )} */}
          
          {activeTab === "results" && (
            <div className="p-4">
              <ResultsTab 
                calculationResult={calculationResult} 
                calculationHistory={calculationHistory}
                currency={currency}
              />
            </div>
          )}
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