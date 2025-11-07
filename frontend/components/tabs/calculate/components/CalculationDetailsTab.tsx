import { Calculator } from "lucide-react"
import { CalculationDetails, CalculationStep } from '../types'

interface CalculationDetailsTabProps {
  calculationDetails: CalculationDetails
  currency: string
  baseTariffAmountUSD: number | null
  convertFromUSD: (amountUSD: number, targetCurrency: string, rates: { [key: string]: number }) => number
  exchangeRates: { [key: string]: number }
}

export function CalculationDetailsTab({
  calculationDetails,
  currency,
  baseTariffAmountUSD,
  convertFromUSD,
  exchangeRates
}: CalculationDetailsTabProps) {
  return (
    <>
      {calculationDetails.productDescription && (
        <div className="p-2 bg-white dark:bg-slate-800 rounded border border-blue-100 dark:border-blue-900">
          <div className="font-medium text-slate-900 dark:text-slate-100 mb-1">Product</div>
          <div className="text-slate-600 dark:text-slate-400 text-xs">
            {calculationDetails.productCode}: {calculationDetails.productDescription}
          </div>
        </div>
      )}

      {calculationDetails.dutyType && (
        <div className="p-2 bg-white dark:bg-slate-800 rounded border border-blue-100 dark:border-blue-900">
          <div className="font-medium text-slate-900 dark:text-slate-100 mb-1">Duty Type</div>
          <div className="text-slate-600 dark:text-slate-400 text-xs">
            {calculationDetails.dutyType}
            {calculationDetails.combinationType && (
              <span className="ml-2 text-purple-600 dark:text-purple-400">
                ({calculationDetails.combinationType})
              </span>
            )}
          </div>
        </div>
      )}

      {calculationDetails.formula && (
        <div className="p-2 bg-white dark:bg-slate-800 rounded border border-blue-100 dark:border-blue-900">
          <div className="font-medium text-slate-900 dark:text-slate-100 mb-1">Formula</div>
          <div className="text-blue-600 dark:text-blue-400 font-mono text-xs">
            {calculationDetails.formula}
          </div>
          {calculationDetails.specificDutyRateRaw && (
            <div className="mt-1 text-slate-600 dark:text-slate-400 text-xs">
              <span className="font-medium">Raw Rate:</span> {calculationDetails.specificDutyRateRaw}
            </div>
          )}
        </div>
      )}

      {calculationDetails.steps && calculationDetails.steps.length > 0 && (
        <div className="p-3 bg-gradient-to-br from-emerald-50 to-teal-50 dark:from-emerald-900/20 dark:to-teal-900/20 rounded border border-emerald-200 dark:border-emerald-800">
          <div className="font-medium text-slate-900 dark:text-slate-100 mb-2 flex items-center gap-2">
            <Calculator className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
            Step-by-Step Calculation
          </div>
          <div className="space-y-2">
            {calculationDetails.steps.map((step: CalculationStep, index: number) => (
              <div 
                key={index} 
                className="flex items-start gap-2 p-2 bg-white dark:bg-slate-800 rounded border border-emerald-100 dark:border-emerald-900"
              >
                <div className="flex-shrink-0 w-6 h-6 rounded-full bg-emerald-600 dark:bg-emerald-700 text-white text-xs font-bold flex items-center justify-center">
                  {step.step}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-medium text-slate-700 dark:text-slate-300 mb-0.5">
                    {step.description}
                  </div>
                  <div className="text-xs font-mono text-emerald-700 dark:text-emerald-400 break-all">
                    {step.value}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {calculationDetails.calculation && (
        <div className="p-2 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded border border-blue-200 dark:border-blue-800">
          <div className="font-medium text-slate-900 dark:text-slate-100 mb-1 text-xs">Full Calculation</div>
          <div className="text-blue-700 dark:text-blue-400 font-mono text-xs break-all">
            {calculationDetails.calculation}
          </div>
        </div>
      )}

      <div className="p-2 bg-gradient-to-r from-blue-100 to-purple-100 dark:from-blue-900/30 dark:to-purple-900/30 rounded border border-blue-300 dark:border-blue-700">
        <div className="font-medium text-slate-900 dark:text-slate-100 mb-1">Final Tariff</div>
        <div className="text-lg font-bold text-blue-600 dark:text-blue-400">
          {currency} {baseTariffAmountUSD !== null ? convertFromUSD(baseTariffAmountUSD, currency, exchangeRates).toFixed(2) : '0.00'}
        </div>
        {currency !== "USD" && baseTariffAmountUSD && (
          <div className="text-xs text-slate-600 dark:text-slate-400 mt-1">
            Base: USD ${baseTariffAmountUSD.toFixed(2)}
          </div>
        )}
      </div>
    </>
  )
}
