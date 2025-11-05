"use client"

import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

interface AdValoremDutyFormProps {
  adValoremRate: number | undefined
  currentValue?: number | null
  onChange: (value: number | undefined) => void
}

export default function AdValoremDutyForm({ 
  adValoremRate, 
  currentValue,
  onChange 
}: AdValoremDutyFormProps) {
  return (
    <div className="space-y-3">
      <div className="text-xs font-semibold text-amber-700 dark:text-amber-300 uppercase tracking-wide mb-2">
        📊 Ad Valorem Duty (Percentage-based)
      </div>
      <div className="grid grid-cols-1 gap-4">
        {/* Ad Valorem Rate */}
        <div className="space-y-1.5">
          <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
            <span>Ad Valorem Rate (%) <span className="text-red-500">*</span></span>
            {currentValue !== undefined && currentValue !== null && (
              <span className="text-xs font-normal text-amber-600 dark:text-amber-400">
                Current: {currentValue}%
              </span>
            )}
          </Label>
          <Input
            type="number"
            step="0.01"
            value={adValoremRate !== undefined ? adValoremRate.toString() : ""}
            onChange={(e) => {
              const value = e.target.value.trim()
              
              if (value === "") {
                onChange(undefined)
              } else {
                const parsed = parseFloat(value)
                if (!isNaN(parsed) && parsed >= 0) {
                  onChange(parsed)
                }
              }
            }}
            placeholder="e.g., 5.5"
            className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500 placeholder:text-slate-400"
          />
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Enter the percentage rate (0-100). Example: 5.5 means 5.5% of the value.
          </p>
        </div>
      </div>
      
      {/* Info Box */}
      <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
        <p className="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-1">
          💡 Ad Valorem Duty Explained
        </p>
        <p className="text-xs text-blue-700 dark:text-blue-300">
          This is a percentage-based tariff applied to the value of goods. For example, if the rate is 5% and the goods are worth $1000, the duty is $50.
        </p>
      </div>
    </div>
  )
}
