import { useState, useEffect, useCallback } from 'react'
import { searchProducts as apiSearchProducts } from '@/lib/api'
import { Product, PREDEFINED_PRODUCTS } from '../types'

export function useProductSearch() {
  const [productSearchQuery, setProductSearchQuery] = useState<string>("")
  const [productSearchResults, setProductSearchResults] = useState<Array<{ code: string, description: string, matchType?: string }>>([])
  const [searchTimeout, setSearchTimeout] = useState<NodeJS.Timeout | null>(null)
  const [selectedProduct, setSelectedProduct] = useState<string>("")

  const searchProducts = useCallback(async (query: string) => {
    try {
      const { ok, data } = await apiSearchProducts(query, 5)

      if (ok && data.products && Array.isArray(data.products)) {
        return data.products.map((p: Product) => ({
          code: p.code || p.tlCode,
          description: p.description || p.name || "No description available",
          matchType: p.matchType
        }))
      }

      const isNumericQuery = /^\d+$/.test(query)
      const filtered = PREDEFINED_PRODUCTS.filter(product =>
        product.code.toLowerCase().includes(query.toLowerCase()) ||
        product.description.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5)

      return filtered.map(product => ({
        ...product,
        matchType: isNumericQuery && product.code.includes(query) ? 'contains_code' : 'description_match'
      }))
    } catch (error: unknown) {
      console.error('Product search error:', error)
      const isNumericQuery = /^\d+$/.test(query)
      const filtered = PREDEFINED_PRODUCTS.filter(product =>
        product.code.toLowerCase().includes(query.toLowerCase()) ||
        product.description.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 5)

      return filtered.map(product => ({
        ...product,
        matchType: isNumericQuery && product.code.includes(query) ? 'contains_code' : 'description_match'
      }))
    }
  }, [])

  useEffect(() => {
    if (searchTimeout) {
      clearTimeout(searchTimeout)
    }

    if (productSearchQuery.length > 0) {
      const timeout = setTimeout(async () => {
        const results = await searchProducts(productSearchQuery)
        setProductSearchResults(results)
      }, 300)

      setSearchTimeout(timeout)
    } else {
      setProductSearchResults([])
    }

    return () => {
      if (searchTimeout) {
        clearTimeout(searchTimeout)
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [productSearchQuery, searchProducts])

  const handleProductSelect = (product: { code: string, description: string, matchType?: string }) => {
    setSelectedProduct(product.code)
    setProductSearchQuery(`${product.code} - ${product.description}`)
    setProductSearchResults([])
  }

  const clearProductSelection = () => {
    setProductSearchQuery("")
    setSelectedProduct("")
    setProductSearchResults([])
  }

  return {
    productSearchQuery,
    setProductSearchQuery,
    productSearchResults,
    selectedProduct,
    handleProductSelect,
    clearProductSelection,
    searchTimeout
  }
}
