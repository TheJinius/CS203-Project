"use client"

import { useState, useEffect } from "react"
import { X } from "lucide-react"
import { Button } from "@/components/ui/button"
import Image from "next/image"
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
  onRouteDetailsChange?: (details: {
    productCode?: string
    productDescription?: string
    tariffAmount?: number
    currency?: string
    sourceCountry?: string
    destinationCountry?: string
  } | undefined) => void
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
  onRouteDetailsChange,
  width
}: SidebarProps) {
  const { isAdmin } = useAuth();
  const { theme } = useTheme();

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
      {/* Header Section - Logo and Close Button Only */}
      <div className="p-4 border-b border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800">
        <div className="flex items-center justify-between">
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
                onRouteDetailsChange={onRouteDetailsChange}
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
                onRouteCalculated={onRouteCalculated}
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