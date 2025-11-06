"use client"

import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

interface CombinedDutyFormProps {
  compoundRate1: number | undefined  // Ad Valorem component
  compoundRate2: number | undefined  // Specific component
  specificRateUnit: string
  combinedMode: 'M' | 'C'  // M = Mixed (max), C = Compound (sum)
  currentRate1?: number | null
  currentRate2?: number | null
  currentUnit?: string | null
  onRate1Change: (value: number | undefined) => void
  onRate2Change: (value: number | undefined) => void
  onUnitChange: (value: string) => void
  onModeChange: (mode: 'M' | 'C') => void
}

export default function CombinedDutyForm({ 
  compoundRate1,
  compoundRate2,
  specificRateUnit,
  combinedMode,
  currentRate1,
  currentRate2,
  currentUnit,
  onRate1Change,
  onRate2Change,
  onUnitChange,
  onModeChange
}: CombinedDutyFormProps) {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between mb-2">
        <div className="text-xs font-semibold text-amber-700 dark:text-amber-300 uppercase tracking-wide">
          🔀 Combined Duty (Ad Valorem + Specific)
        </div>
        <Select value={combinedMode} onValueChange={(v) => onModeChange(v as 'M' | 'C')}>
          <SelectTrigger className="w-[180px] h-8 text-xs border-2 border-amber-300 dark:border-amber-700">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="M">Mixed (Maximum)</SelectItem>
            <SelectItem value="C">Compound (Sum)</SelectItem>
          </SelectContent>
        </Select>
      </div>
      
      <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-md mb-3">
        <p className="text-xs text-blue-900 dark:text-blue-100">
          <strong>{combinedMode === 'M' ? 'Mixed:' : 'Compound:'}</strong>{' '}
          {combinedMode === 'M' 
            ? 'The higher of the two duty components is applied'
            : 'Both duty components are added together'}
        </p>
      </div>
      
      <div className="grid grid-cols-2 gap-4">
        {/* Compound Rate 1 (Ad Valorem component) */}
        <div className="space-y-1.5">
          <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
            <span>Ad Valorem Component (%) <span className="text-red-500">*</span></span>
            {currentRate1 !== undefined && currentRate1 !== null && (
              <span className="text-xs font-normal text-amber-600 dark:text-amber-400">
                Current: {currentRate1}%
              </span>
            )}
          </Label>
          <Input
            type="number"
            step="0.01"
            value={compoundRate1 !== undefined ? compoundRate1.toString() : ""}
            onChange={(e) => {
              const value = e.target.value.trim()
              
              if (value === "") {
                onRate1Change(undefined)
              } else {
                const parsed = parseFloat(value)
                if (!isNaN(parsed) && parsed >= 0) {
                  onRate1Change(parsed)
                }
              }
            }}
            placeholder="e.g., 5.5"
            className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500 placeholder:text-slate-400"
          />
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Percentage-based component
          </p>
        </div>

        {/* Compound Rate 2 (Specific component) */}
        <div className="space-y-1.5">
          <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
            <span>Specific Component <span className="text-red-500">*</span></span>
            {currentRate2 !== undefined && currentRate2 !== null && (
              <span className="text-xs font-normal text-amber-600 dark:text-amber-400">
                Current: {currentRate2}
              </span>
            )}
          </Label>
          <Input
            type="number"
            step="0.01"
            value={compoundRate2 !== undefined ? compoundRate2.toString() : ""}
            onChange={(e) => {
              const value = e.target.value.trim()
              
              if (value === "") {
                onRate2Change(undefined)
              } else {
                const parsed = parseFloat(value)
                if (!isNaN(parsed) && parsed >= 0) {
                  onRate2Change(parsed)
                }
              }
            }}
            placeholder="e.g., 10.50"
            className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500 placeholder:text-slate-400"
          />
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Fixed amount component
          </p>
        </div>
        
        {/* Unit for Specific Component */}
        <div className="space-y-1.5 col-span-2">
          <Label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
            Unit for Specific Component <span className="text-red-500">*</span>
          </Label>
          <Input
            value={specificRateUnit}
            onChange={(e) => onUnitChange(e.target.value)}
            placeholder="e.g., kg, liter, unit"
            className="border-2 border-amber-300 dark:border-amber-700 focus:border-amber-500 placeholder:text-slate-400"
          />
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Unit of measurement for the specific component
          </p>
        </div>
      </div>
      
      {/* Info Box */}
      <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
        <p className="text-xs font-semibold text-blue-900 dark:text-blue-100 mb-1">
          💡 Combined Duty Explained
        </p>
        <p className="text-xs text-blue-700 dark:text-blue-300 mb-2">
          This combines both percentage and fixed amount duties.
        </p>
        <ul className="text-xs text-blue-700 dark:text-blue-300 ml-4 space-y-1 list-disc">
          <li><strong>Mixed (M):</strong> Whichever is higher - 5% of value OR $10/kg</li>
          <li><strong>Compound (C):</strong> Both added - 5% of value + $10/kg</li>
        </ul>
      </div>
    </div>
  )
}
