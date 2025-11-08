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
  "000": "World (Any Country)",
  "032": "Argentina",
  "036": "Australia",
  "048": "Bahrain",
  "050": "Bangladesh",
  "056": "Belgium",
  "076": "Brazil",
  "124": "Canada",
  "144": "Sri Lanka",
  "152": "Chile",
  "156": "China",
  "158": "Taiwan",
  "192": "Cuba",
  "208": "Denmark",
  "214": "Dominican Republic",
  "246": "Finland",
  "250": "France",
  "276": "Germany",
  "344": "Hong Kong",
  "348": "Hungary",
  "356": "India",
  "360": "Indonesia",
  "364": "Iran",
  "368": "Iraq",
  "376": "Israel",
  "380": "Italy",
  "392": "Japan",
  "404": "Kenya",
  "410": "South Korea",
  "414": "Kuwait",
  "458": "Malaysia",
  "484": "Mexico",
  "512": "Oman",
  "528": "Netherlands",
  "554": "New Zealand",
  "578": "Norway",
  "586": "Pakistan",
  "604": "Peru",
  "608": "Philippines",
  "616": "Belgium",
  "634": "Qatar",
  "682": "Saudi Arabia",
  "702": "Singapore",
  "704": "Vietnam",
  "710": "South Africa",
  "724": "Spain",
  "752": "Sweden",
  "764": "Thailand",
  "784": "UAE",
  "792": "Turkey",
  "818": "Egypt",
  "826": "United Kingdom",
  "840": "United States",
  "862": "Venezuela"
}

export const PREDEFINED_PRODUCTS = [
  { code: "27079940", description: "Carbazole, Energy" },
  { code: "1012100", description: "Pure Bred Breeding Horses" },
  { code: "29092000", description: "Cyclanic, Pharmaceutical" },
  { code: "74130000", description: "Copper Wire" }
]
