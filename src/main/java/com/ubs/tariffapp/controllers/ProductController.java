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
            // Get all products and filter by query
            List<Product> allProducts = productRepository.findAll();
            
            List<Product> matchingProducts = allProducts.stream()
                    .filter(product -> 
                        (product.getTlCode() != null && product.getTlCode().toLowerCase().contains(query.toLowerCase())) ||
                        (product.getDescription() != null && product.getDescription().toLowerCase().contains(query.toLowerCase()))
                    )
                    .limit(limit)
                    .collect(Collectors.toList());

            // Convert to response format
            List<Map<String, Object>> productList = matchingProducts.stream()
                    .map(product -> {
                        Map<String, Object> productMap = new HashMap<>();
                        productMap.put("code", product.getTlCode());
                        productMap.put("description", product.getDescription());
                        return productMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("products", productList);
            response.put("count", productList.size());
            response.put("query", query);
            response.put("status", "success");

            System.out.println("✅ Found " + productList.size() + " product(s)");
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
}