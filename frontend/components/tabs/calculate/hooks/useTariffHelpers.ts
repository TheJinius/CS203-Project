import { Tariff } from '../types'

export function useTariffHelpers() {
  const convertFromUSD = (amountUSD: number, targetCurrency: string, rates: { [key: string]: number }): number => {
    if (targetCurrency === "USD") return amountUSD
    const rate = rates[targetCurrency] || 1
    return Math.round(amountUSD * rate * 100) / 100
  }

  const getLowestTariffId = (availableTariffs: Tariff[]): number | null => {
    if (availableTariffs.length === 0) return null
    
    const freeTradeTariff = availableTariffs.find(tariff => {
      const desc = tariff.description?.toLowerCase() || ''
      return desc.includes('0%') || desc.includes('free') || tariff.rate === 0
    })
    
    if (freeTradeTariff) return freeTradeTariff.tariffId
    
    const adValoremTariffs = availableTariffs.filter(t => t.dutyClass === 'AdValoremDuty' && t.rate !== undefined)
    
    if (adValoremTariffs.length > 0) {
      const lowestAdValorem = adValoremTariffs.reduce((lowest, current) => {
        return (current.rate || Infinity) < (lowest.rate || Infinity) ? current : lowest
      })
      return lowestAdValorem.tariffId
    }
    
    return availableTariffs[0]?.tariffId || null
  }

  const getPriorityColor = (category: string) => {
    const lowerCategory = category.toLowerCase()
    if (lowerCategory.includes('high') || lowerCategory.includes('critical') || lowerCategory.includes('mandatory')) {
      return 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400'
    } else if (lowerCategory.includes('medium') || lowerCategory.includes('important')) {
      return 'bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400'
    } else {
      return 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
    }
  }

  return {
    convertFromUSD,
    getLowestTariffId,
    getPriorityColor
  }
}
