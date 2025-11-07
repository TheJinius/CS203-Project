import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Tariff } from '../types'

interface AmountInputFieldsProps {
  selectedTariff: string
  availableTariffs: Tariff[]
  amountOfProduct: string
  setAmountOfProduct: (value: string) => void
  productValueDollars: string
  setProductValueDollars: (value: string) => void
  productQuantity: string
  setProductQuantity: (value: string) => void
}

export function AmountInputFields({
  selectedTariff,
  availableTariffs,
  amountOfProduct,
  setAmountOfProduct,
  productValueDollars,
  setProductValueDollars,
  productQuantity,
  setProductQuantity
}: AmountInputFieldsProps) {
  if (!selectedTariff) return null

  const tariff = availableTariffs.find(t => t.tariffId.toString() === selectedTariff)
  const isCombinedDuty = tariff?.dutyClass === 'CombinedDuty'

  if (isCombinedDuty) {
    return (
      <>
        <div className="space-y-1.5">
          <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
            Product Value for Ad Valorem
            <span className="text-blue-600 dark:text-blue-400"> (in dollars, $)</span>
          </Label>
          <Input
            type="number"
            value={productValueDollars}
            onChange={(e) => setProductValueDollars(e.target.value)}
            placeholder="Enter dollar value (e.g., 10000)"
            step="0.01"
            min="0"
            className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-sm font-medium"
          />
        </div>
        
        <div className="space-y-1.5">
          <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
            Product Quantity for Specific Duty
            {tariff.unit && <span className="text-blue-600 dark:text-blue-400"> (in {tariff.unit})</span>}
          </Label>
          <Input
            type="number"
            value={productQuantity}
            onChange={(e) => setProductQuantity(e.target.value)}
            placeholder={`Enter quantity in ${tariff.unit || 'units'} (e.g., 1000)`}
            step="0.01"
            min="0"
            className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-sm font-medium"
          />
        </div>
      </>
    )
  }

  const isAdValorem = tariff?.dutyClass === 'AdValoremDuty'

  return (
    <div className="space-y-1.5">
      <Label className="text-sm font-medium text-slate-700 dark:text-slate-300">
        Amount of Product
        {isAdValorem ? (
          <span className="text-blue-600 dark:text-blue-400"> (in dollars, $)</span>
        ) : tariff?.unit ? (
          <span className="text-blue-600 dark:text-blue-400"> (in {tariff.unit})</span>
        ) : null}
      </Label>
      <Input
        type="number"
        value={amountOfProduct}
        onChange={(e) => setAmountOfProduct(e.target.value)}
        placeholder={
          isAdValorem 
            ? "Enter dollar value (e.g., 10000)"
            : tariff?.unit 
            ? `Enter quantity in ${tariff.unit} (e.g., 1000)`
            : "Enter quantity/amount (e.g., 1000)"
        }
        step="0.01"
        min="0"
        className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 text-sm font-medium"
      />
    </div>
  )
}
