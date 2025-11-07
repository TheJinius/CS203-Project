// Shared types for Calculate Tab components

export interface Product {
  code: string
  tlCode?: string
  description?: string
  name?: string
  matchType?: string
}

export interface Tariff {
  tariffId: number
  description?: string
  dutyType?: string
  dutyClass?: string
  unit?: string
  rate?: number
  amountPerUnit?: number
}

export interface CalculationStep {
  step: string
  description: string
  value: string
}

export interface CalculationDetails {
  tariffAmount: number
  currency: string
  tariffId: number
  status: string
  dutyType?: string
  dutyTypeCode?: string
  productDescription?: string
  productCode?: string
  formula?: string
  calculation?: string
  steps?: CalculationStep[]
  rate?: number
  rateDisplay?: string
  productValue?: number
  tariffResult?: number
  amountPerUnit?: number
  amountPerUnitDisplay?: string
  multiplier?: number
  unit?: string
  productQuantity?: number
  billingUnits?: number
  billingUnitsDisplay?: string
  specificDutyRateRaw?: string
  adValoremRate?: number
  adValoremRateDisplay?: string
  adValoremProductValue?: number
  adValoremAmount?: number
  specificAmountPerUnit?: number
  specificAmountPerUnitDisplay?: string
  specificMultiplier?: number
  specificUnit?: string
  specificProductQuantity?: number
  specificBillingUnits?: number
  specificBillingUnitsDisplay?: string
  specificAmount?: number
  mixedOrCompound?: string
  combinationType?: string
  combinationLogic?: string
  error?: string
}

export interface ComplianceTask {
  country: string
  sector: string
  task_category: string
  task_name: string
  description: string
  responsible_agency: string
  compliance_requirement: string
  timing: string
  reference: string
  reference_url: string
}

export const COUNTRY_NAMES: { [key: string]: string } = {
  "702": "Singapore",
  "840": "United States",
  "156": "China",
  "000": "World (Any Country)"
}

export const PREDEFINED_PRODUCTS = [
  { code: "27079940", description: "Carbazole, Energy" },
  { code: "1012100", description: "Pure Bred Breeding Horses" },
  { code: "29092000", description: "Cyclanic, Pharmaceutical" },
  { code: "74130000", description: "Copper Wire" }
]
