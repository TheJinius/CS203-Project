import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tariff } from '../types'

interface TariffSelectorProps {
  availableTariffs: Tariff[]
  selectedTariff: string
  setSelectedTariff: (value: string) => void
  getLowestTariffId: () => number | null
}

export function TariffSelector({ 
  availableTariffs, 
  selectedTariff, 
  setSelectedTariff,
  getLowestTariffId 
}: TariffSelectorProps) {
  return (
    <div className="space-y-1.5">
      <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
        Select Tariff
      </Label>
      <Select onValueChange={setSelectedTariff} value={selectedTariff}>
        <SelectTrigger className="w-full min-h-25 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
          <SelectValue placeholder="Choose a tariff" />
        </SelectTrigger>
        <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600 max-h-60 overflow-y-auto [width:var(--radix-select-trigger-width)]">
          {availableTariffs.map(tariff => {
            const lowestTariffId = getLowestTariffId()
            const isLowest = tariff.tariffId === lowestTariffId
            const desc = tariff.description?.toLowerCase() || ''
            const isFTA = desc.includes('0%') || desc.includes('free') || tariff.rate === 0
            
            return (
              <SelectItem
                key={tariff.tariffId}
                value={tariff.tariffId.toString()}
                className={`!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20 text-sm ${
                  isLowest ? 'border-2 border-green-500 dark:border-green-400 bg-green-50 dark:bg-green-900/20 hover:!bg-green-100 dark:hover:!bg-green-900/30' : ''
                }`}>
                <div className="flex flex-col gap-0.5 py-1">
                  <div className="flex items-center gap-2">
                    <span className="font-medium">Tariff ID: {tariff.tariffId}</span>
                    {isLowest && isFTA && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-green-600 dark:bg-green-500 text-white">
                        FREE TRADE
                      </span>
                    )}
                    {isLowest && !isFTA && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-600 dark:bg-emerald-500 text-white">
                        LOWEST RATE
                      </span>
                    )}
                  </div>
                  {tariff.description && (
                    <span className="text-xs text-slate-500 dark:text-slate-400 whitespace-normal">
                      {tariff.description}
                    </span>
                  )}
                </div>
              </SelectItem>
            )
          })}
        </SelectContent>
      </Select>
    </div>
  )
}
