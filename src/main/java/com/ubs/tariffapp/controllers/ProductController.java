package com.ubs.tariffapp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.repositories.ProductRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        
        System.out.println("🔍 Searching products with query: " + query + ", limit: " + limit);

        try {
            // Get all products from database
            List<Product> allProducts = productRepository.findAll();
            
            // Determine if query looks like an HS Code (numeric) or text description
            boolean isNumericQuery = query.matches("\\d+");
            
            List<Product> matchingProducts;
            
            if (isNumericQuery) {
                // For numeric queries, prioritize exact HS Code matches, then partial matches
                matchingProducts = allProducts.stream()
                        .filter(product -> product.getTlCode() != null)
                        .sorted((p1, p2) -> {
                            // Exact match gets highest priority
                            boolean p1Exact = p1.getTlCode().equals(query);
                            boolean p2Exact = p2.getTlCode().equals(query);
                            if (p1Exact && !p2Exact) return -1;
                            if (!p1Exact && p2Exact) return 1;
                            
                            // Starts with query gets second priority
                            boolean p1Starts = p1.getTlCode().startsWith(query);
                            boolean p2Starts = p2.getTlCode().startsWith(query);
                            if (p1Starts && !p2Starts) return -1;
                            if (!p1Starts && p2Starts) return 1;
                            
                            // Contains query gets third priority
                            boolean p1Contains = p1.getTlCode().contains(query);
                            boolean p2Contains = p2.getTlCode().contains(query);
                            if (p1Contains && !p2Contains) return -1;
                            if (!p1Contains && p2Contains) return 1;
                            
                            return 0;
                        })
                        .filter(product -> 
                            product.getTlCode().contains(query) ||
                            (product.getDescription() != null && 
                             product.getDescription().toLowerCase().contains(query.toLowerCase()))
                        )
                        .limit(limit)
                        .collect(Collectors.toList());
            } else {
                // For text queries, search primarily in descriptions with relevance scoring
                matchingProducts = allProducts.stream()
                        .filter(product -> 
                            (product.getDescription() != null && 
                             product.getDescription().toLowerCase().contains(query.toLowerCase())) ||
                            (product.getTlCode() != null && 
                             product.getTlCode().toLowerCase().contains(query.toLowerCase()))
                        )
                        .sorted((p1, p2) -> {
                            // Calculate relevance scores for descriptions
                            int p1Score = calculateRelevanceScore(p1, query);
                            int p2Score = calculateRelevanceScore(p2, query);
                            return Integer.compare(p2Score, p1Score); // Higher score first
                        })
                        .limit(limit)
                        .collect(Collectors.toList());
            }

            // Convert to response format with additional metadata
            List<Map<String, Object>> productList = matchingProducts.stream()
                    .map(product -> {
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("code", product.getTlCode());
                        productMap.put("description", product.getDescription());
                        
                        // Add match type for frontend to understand the search result
                        String matchType = determineMatchType(product, query, isNumericQuery);
                        productMap.put("matchType", matchType);
                        
                        return productMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("products", productList);
            response.put("count", productList.size());
            response.put("query", query);
            response.put("searchType", isNumericQuery ? "hsCode" : "description");
            response.put("status", "success");

            System.out.println("✅ Found " + productList.size() + " product(s) using " + 
                             (isNumericQuery ? "HS Code" : "description") + " search");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Product search error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            errorResponse.put("status", "error");
            errorResponse.put("products", List.of());
            errorResponse.put("count", 0);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // Helper method to calculate relevance score for text-based searches
    private int calculateRelevanceScore(Product product, String query) {
        int score = 0;
        String lowerQuery = query.toLowerCase();
        
        if (product.getDescription() != null) {
            String lowerDescription = product.getDescription().toLowerCase();
            
            // Exact phrase match gets highest score
            if (lowerDescription.contains(lowerQuery)) {
                score += 10;
            }
            
            // Count individual word matches
            String[] queryWords = lowerQuery.split("\\s+");
            for (String word : queryWords) {
                if (lowerDescription.contains(word)) {
                    score += 3;
                }
            }
            
            // Boost score if match is at the beginning
            if (lowerDescription.startsWith(lowerQuery)) {
                score += 5;
            }
        }
        
        // Also check HS Code for additional matches
        if (product.getTlCode() != null && product.getTlCode().toLowerCase().contains(lowerQuery)) {
            score += 2;
        }
        
        return score;
    }
    
    // Helper method to determine what type of match this is
    private String determineMatchType(Product product, String query, boolean isNumericQuery) {
        if (isNumericQuery && product.getTlCode() != null) {
            if (product.getTlCode().equals(query)) {
                return "exact_code";
            } else if (product.getTlCode().startsWith(query)) {
                return "starts_with_code";
            } else if (product.getTlCode().contains(query)) {
                return "contains_code";
            }
        }
        
        if (product.getDescription() != null && 
            product.getDescription().toLowerCase().contains(query.toLowerCase())) {
            return "description_match";
        }
        
        return "partial_match";
    }
}