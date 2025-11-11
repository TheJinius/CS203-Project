import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Search } from "lucide-react"
import { CountrySearchSelect } from './CountrySearchSelect'

interface SearchFormProps {
  selectedSource: string
  setSelectedSource: (value: string) => void
  selectedDestination: string
  setSelectedDestination: (value: string) => void
  productSearchQuery: string
  setProductSearchQuery: (value: string) => void
  productSearchResults: Array<{ code: string, description: string, matchType?: string }>
  selectedProduct: string
  handleProductSelect: (product: { code: string, description: string, matchType?: string }) => void
  clearProductSelection: () => void
  searchTimeout: NodeJS.Timeout | null
  selectedYear: string
  setSelectedYear: (value: string) => void
  loading: boolean
  onSearch: () => void
}

export function SearchForm({
  selectedSource,
  setSelectedSource,
  selectedDestination,
  setSelectedDestination,
  productSearchQuery,
  setProductSearchQuery,
  productSearchResults,
  selectedProduct,
  handleProductSelect,
  clearProductSelection,
  searchTimeout,
  selectedYear,
  setSelectedYear,
  loading,
  onSearch
}: SearchFormProps) {
  return (
    <>
      <CountrySearchSelect
        label="Source Country (Partner)"
        value={selectedSource}
        onValueChange={setSelectedSource}
        placeholder="Search source country..."
        id="source"
        excludeValue={selectedDestination}
      />

      <CountrySearchSelect
        label="Destination Country (Reporter)"
        value={selectedDestination}
        onValueChange={setSelectedDestination}
        placeholder="Search destination country..."
        id="destination"
        excludeValue={selectedSource}
      />

      <div className="space-y-1.5">
        <Label htmlFor="product" className="text-sm font-medium text-slate-700 dark:text-slate-300">
          Product Search
        </Label>
        <div className="relative">
          <Input
            type="text"
            value={productSearchQuery}
            onChange={(e) => setProductSearchQuery(e.target.value)}
            placeholder="Search by HS Code or description"
            className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400 pr-16"
          />
          {productSearchQuery && selectedProduct && (
            <button
              type="button"
              onClick={clearProductSelection}
              className="absolute right-2 top-2 text-slate-400 hover:text-slate-600 dark:text-slate-500 dark:hover:text-slate-300"
              title="Clear selection"
            >
              ✕
            </button>
          )}
          {productSearchResults.length > 0 && productSearchQuery && (
            <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-md shadow-lg max-h-60 overflow-y-auto">
              {productSearchResults.map((product, index) => (
                <button
                  key={`${product.code}-${index}`}
                  type="button"
                  onClick={() => handleProductSelect(product)}
                  className="w-full text-left px-3 py-2 hover:bg-slate-100 dark:hover:bg-slate-700 text-slate-900 dark:text-slate-100 border-b border-slate-200 dark:border-slate-600 last:border-b-0"
                >
                  <div className="flex items-center justify-between">
                    <div className="font-medium text-blue-600 dark:text-blue-400">{product.code}</div>
                    {product.matchType}
                  </div>
                  <div className="text-sm text-slate-600 dark:text-slate-400 mt-1 line-clamp-2">
                    {product.description || "No description available"}
                  </div>
                </button>
              ))}
            </div>
          )}
          {productSearchQuery && productSearchResults.length === 0 && searchTimeout === null && (
            <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-md shadow-lg p-3 text-center text-sm text-slate-500 dark:text-slate-400">
              No products found matching &quot;{productSearchQuery}&quot;
            </div>
          )}
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="year" className="text-sm font-medium text-slate-700 dark:text-slate-300">
          Year
        </Label>
        <Select onValueChange={setSelectedYear} value={selectedYear}>
          <SelectTrigger className="w-full h-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400">
            <SelectValue placeholder="Select year" />
          </SelectTrigger>
          <SelectContent className="bg-white dark:bg-slate-800 border-slate-300 dark:border-slate-600">
            <SelectItem value="2025" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
              2025
            </SelectItem>
            <SelectItem value="2024" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
              2024
            </SelectItem>
            <SelectItem value="2023" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
              2023
            </SelectItem>
            <SelectItem value="2022" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
              2022
            </SelectItem>
            <SelectItem value="2021" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
              2021
            </SelectItem>
            <SelectItem value="2020" className="!text-slate-900 dark:!text-slate-100 hover:bg-slate-100 dark:hover:bg-slate-700 focus:bg-blue-50 dark:focus:bg-blue-900/20">
              2020
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      <Button
        onClick={onSearch}
        disabled={loading || !selectedProduct || !selectedSource || !selectedDestination}
        className="w-full h-9 mt-4 bg-blue-600 hover:bg-blue-700 dark:bg-blue-600 dark:hover:bg-blue-700 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
      >
        <Search className="h-4 w-4" />
        {loading ? "Searching..." : `Search Available Tariffs for ${selectedYear}`}
      </Button>
    </>
  )
}
