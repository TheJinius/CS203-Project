import { useState, useEffect, useRef } from "react"
import { Label } from "@/components/ui/label"
import { Input } from "@/components/ui/input"
import { Search, X } from "lucide-react"
import { getCountries } from '@/lib/api'

interface CountrySearchSelectProps {
  label: string
  value: string
  onValueChange: (value: string) => void
  placeholder?: string
  id?: string
  excludeValue?: string
}

export function CountrySearchSelect({
  label,
  value,
  onValueChange,
  placeholder = "Search country...",
  id,
  excludeValue
}: CountrySearchSelectProps) {
  const [searchQuery, setSearchQuery] = useState("")
  const [isOpen, setIsOpen] = useState(false)
  const [filteredCountries, setFilteredCountries] = useState<Array<{ code: string; name: string }>>([])
  const [allCountries, setAllCountries] = useState<Array<{ code: string; name: string }>>([])
  const [loading, setLoading] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Fetch countries from database on component mount
  useEffect(() => {
    const fetchCountries = async () => {
      setLoading(true)
      try {
        const { ok, data } = await getCountries()
        if (ok && data.countries) {
          // Sort countries: World (000) first, then alphabetically by name
          const sorted = [...data.countries].sort((a, b) => {
            if (a.code === "000") return -1
            if (b.code === "000") return 1
            return a.name.localeCompare(b.name)
          })
          setAllCountries(sorted)
          setFilteredCountries(sorted.filter(c => c.code !== excludeValue))
        } else {
          console.error('Failed to load countries:', data.error)
        }
      } catch (error) {
        console.error('Error fetching countries:', error)
      } finally {
        setLoading(false)
      }
    }
    
    fetchCountries()
  }, [])

  // Filter countries based on search query and excludeValue
  useEffect(() => {
    const availableCountries = allCountries.filter(country => country.code !== excludeValue)
    
    if (searchQuery.trim() === "") {
      setFilteredCountries(availableCountries)
    } else {
      const query = searchQuery.toLowerCase()
      const filtered = availableCountries.filter(
        country =>
          country.name.toLowerCase().includes(query) ||
          country.code.includes(query)
      )
      setFilteredCountries(filtered)
    }
  }, [searchQuery, allCountries, excludeValue])

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false)
      }
    }

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside)
      return () => document.removeEventListener("mousedown", handleClickOutside)
    }
  }, [isOpen])

  const handleSelect = (code: string) => {
    onValueChange(code)
    setSearchQuery("")
    setIsOpen(false)
  }

  const handleClear = () => {
    onValueChange("")
    setSearchQuery("")
    inputRef.current?.focus()
  }

  const selectedCountry = value ? allCountries.find(c => c.code === value) : null

  return (
    <div className="space-y-1.5" ref={dropdownRef}>
      <Label htmlFor={id} className="text-sm font-medium text-slate-700 dark:text-slate-300">
        {label}
      </Label>
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400 dark:text-slate-400 pointer-events-none" />
        <Input
          ref={inputRef}
          type="text"
          value={value && !isOpen && selectedCountry ? `${value} - ${selectedCountry.name}` : searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value)
            if (!isOpen) setIsOpen(true)
          }}
          placeholder={loading ? "Loading countries..." : placeholder}
          className="w-full h-9 pl-9 pr-9 bg-white dark:bg-slate-700 border-slate-300 dark:border-slate-600 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-400 hover:border-slate-400 dark:hover:border-slate-500 focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400"
          onFocus={() => {
            if (value) {
              setSearchQuery("")
            }
            setIsOpen(true)
          }}
          readOnly={Boolean(value && !isOpen)}
          disabled={loading}
        />
        {value && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              handleClear()
            }}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:text-slate-400 dark:hover:text-slate-300"
          >
            <X className="h-4 w-4" />
          </button>
        )}

        {isOpen && (
          <div className="absolute z-50 w-full mt-1 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-md shadow-lg max-h-60 overflow-y-auto">
            {loading ? (
              <div className="px-3 py-4 text-center text-sm text-slate-500 dark:text-slate-400">
                Loading countries...
              </div>
            ) : filteredCountries.length > 0 ? (
              filteredCountries.map((country) => (
                <button
                  key={country.code}
                  type="button"
                  onClick={() => handleSelect(country.code)}
                  className={`w-full text-left px-3 py-2 hover:bg-slate-100 dark:hover:bg-slate-700 border-b border-slate-200 dark:border-slate-600 last:border-b-0 ${
                    value === country.code
                      ? 'bg-blue-50 dark:bg-blue-900/20'
                      : ''
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-blue-600 dark:text-blue-400 text-sm">
                      {country.code}
                    </span>
                  </div>
                  <div className="text-sm text-slate-900 dark:text-slate-100 mt-0.5">
                    {country.name}
                  </div>
                </button>
              ))
            ) : (
              <div className="px-3 py-4 text-center text-sm text-slate-500 dark:text-slate-400">
                No countries found matching &quot;{searchQuery}&quot;
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
