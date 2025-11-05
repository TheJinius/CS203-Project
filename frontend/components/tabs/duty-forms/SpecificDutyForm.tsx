"use client"

import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

interface SpecificDutyFormProps {
  specificRate: number | undefined
  specificRateUnit: string
  currentRate?: number | null
  currentUnit?: string | null
  onRateChange: (value: number | undefined) => void
  onUnitChange: (value: string) => void
}

export default function SpecificDutyForm({ 
  specificRate,
  specificRateUnit,
  currentRate,
  currentUnit,
  onRateChange,
  onUnitChange
}: SpecificDutyFormProps) {
  return (
    <div className="space-y-3">
      <div className="text-xs font-semibold text-amber-700 dark:text-amber-300 uppercase tracking-wide mb-2">
        📦 Specific Duty (Fixed Amount per Unit)
      </div>
      <div className="grid grid-cols-2 gap-4">
        {/* Specific Rate */}
        <div className="space-y-1.5">
          <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
            <span>Specific Rate <span className="text-red-500">*</span></span>
            {currentRate !== undefined && currentRate !== null && (
              <span className="text-xs font-normal text-amber-600 dark:text-amber-400">
                Current: {currentRate}
              </span>
            )}
          </Label>
          <Input
            type="number"
            step="0.01"
            value={specificRate !== undefined ? specificRate.toString() : ""}
            onChange={(e) => {
              const value = e.target.value.trim()
              
              if (value === "") {
                onRateChange(undefined)
              } else {
                const parsed = parseFloat(value)
                if (!isNaN(parsed) && parsed >= 0) {
                  onRateChange(parsed)
                }
              }
            }}
            placeholder="e.g., 10.50"
            className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500 placeholder:text-slate-400"
          />
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Fixed amount charged per unit
          </p>
        </div>
        
        {/* Unit */}
        <div className="space-y-1.5">
          <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
            Unit <span className="text-red-500">*</span>
          </Label>
          <Input
            value={specificRateUnit}
            onChange={(e) => onUnitChange(e.target.value)}
            placeholder="e.g., kg, liter, unit"
            className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500 placeholder:text-slate-400"
          />
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Unit of measurement (kg, liter, etc.)
          </p>
        </div>
      </div>
      
      {/* Info Box */}
      <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
        <p className="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-1">
          💡 Specific Duty Explained
        </p>
        <p className="text-xs text-blue-700 dark:text-blue-300">
          This is a fixed tariff per unit of goods. For example, if the rate is $10 per kg and you import 100 kg, the duty is $1000.
        </p>
      </div>
    </div>
  )
}
