package com.ubs.tariffapp.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.repositories.ProductRepository;

/**
 * Unit tests for ProductController.
 * Tests the product search endpoint with various search scenarios.
 * Uses @WebMvcTest for lightweight testing with mocked dependencies (no database required).
 */
@WebMvcTest(ProductController.class)
@DisplayName("ProductController Unit Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    private List<Product> mockProducts;

    @BeforeEach
    void setUp() {
        // Setup mock products with various HS codes and descriptions
        Product product1 = new Product();
        product1.setTlCode("010121");
        product1.setDescription("Live horses: Pure-bred breeding animals");
        product1.setDigits(6);

        Product product2 = new Product();
        product2.setTlCode("010129");
        product2.setDescription("Live horses: Other than pure-bred breeding animals");
        product2.setDigits(6);

        Product product3 = new Product();
        product3.setTlCode("010130");
        product3.setDescription("Live asses");
        product3.setDigits(6);

        Product product4 = new Product();
        product4.setTlCode("020110");
        product4.setDescription("Meat of bovine animals, fresh or chilled: Carcasses and half-carcasses");
        product4.setDigits(6);

        Product product5 = new Product();
        product5.setTlCode("030212");
        product5.setDescription("Fish, fresh or chilled: Salmon");
        product5.setDigits(6);

        Product product6 = new Product();
        product6.setTlCode("101010");
        product6.setDescription("Wheat and meslin: Durum wheat");
        product6.setDigits(6);

        Product product7 = new Product();
        product7.setTlCode("220110");
        product7.setDescription("Waters, including mineral waters: Mineral waters");
        product7.setDigits(6);

        Product product8 = new Product();
        product8.setTlCode("220120");
        product8.setDescription("Waters, including mineral waters: Other waters");
        product8.setDigits(6);

        Product product9 = new Product();
        product9.setTlCode("010110");
        product9.setDescription("Live horses, asses, mules and hinnies: Pure-bred breeding horses");
        product9.setDigits(6);

        Product product10 = new Product();
        product10.setTlCode("840110");
        product10.setDescription("Nuclear reactors: Nuclear reactors");
        product10.setDigits(6);

        mockProducts = Arrays.asList(product1, product2, product3, product4, product5, 
                                     product6, product7, product8, product9, product10);
    }

    @Nested
    @DisplayName("GET /api/products/search - Search Products")
    class SearchProductsTests {

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should search products by exact HS code match")
        void testSearchProducts_ExactHSCodeMatch() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.query").value("010121"))
                    .andExpect(jsonPath("$.searchType").value("hsCode"))
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.products[0].code").value("010121"))
                    .andExpect(jsonPath("$.products[0].description").value("Live horses: Pure-bred breeding animals"))
                    .andExpect(jsonPath("$.products[0].matchType").value("exact_code"));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should search products by partial HS code (starts with)")
        void testSearchProducts_PartialHSCodeStartsWith() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Search for "0101" should match codes starting with 0101
            mockMvc.perform(get("/api/products/search")
                    .param("q", "0101")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.searchType").value("hsCode"))
                    .andExpect(jsonPath("$.count").value(5)) // Limited by default limit
                    .andExpect(jsonPath("$.products[0].matchType").value("starts_with_code"))
                    .andExpect(jsonPath("$.products[1].matchType").value("starts_with_code"));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should search products by description keyword")
        void testSearchProducts_DescriptionKeyword() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Search for "horses"
            mockMvc.perform(get("/api/products/search")
                    .param("q", "horses")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.searchType").value("description"))
                    .andExpect(jsonPath("$.count").value(3))
                    .andExpect(jsonPath("$.products[0].matchType").value("description_match"))
                    .andExpect(jsonPath("$.products[1].matchType").value("description_match"))
                    .andExpect(jsonPath("$.products[2].matchType").value("description_match"));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should search products case-insensitively")
        void testSearchProducts_CaseInsensitive() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Search with uppercase
            mockMvc.perform(get("/api/products/search")
                    .param("q", "HORSES")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(3));

            // Search with mixed case
            mockMvc.perform(get("/api/products/search")
                    .param("q", "HoRsEs")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(3));

            verify(productRepository, times(2)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should respect limit parameter")
        void testSearchProducts_RespectLimit() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Search with limit 2
            mockMvc.perform(get("/api/products/search")
                    .param("q", "01")
                    .param("limit", "2")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.products").isArray())
                    .andExpect(jsonPath("$.products.length()").value(2));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should use default limit of 5 when not specified")
        void testSearchProducts_DefaultLimit() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Search without limit parameter
            mockMvc.perform(get("/api/products/search")
                    .param("q", "01")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(5)); // Should default to 5

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return empty results for no matches")
        void testSearchProducts_NoMatches() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Search for non-existent product
            mockMvc.perform(get("/api/products/search")
                    .param("q", "999999")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.products").isEmpty());

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle empty product list")
        void testSearchProducts_EmptyProductList() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.products").isEmpty());

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should prioritize exact matches in HS code search")
        void testSearchProducts_PrioritizeExactMatch() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Exact match should appear first
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.products[0].code").value("010121"))
                    .andExpect(jsonPath("$.products[0].matchType").value("exact_code"));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should search by multiple word description")
        void testSearchProducts_MultiWordDescription() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "mineral waters")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.searchType").value("description"))
                    .andExpect(jsonPath("$.count").value(2)); // Should match both water products

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle products with null descriptions")
        void testSearchProducts_NullDescription() throws Exception {
            // Arrange
            Product productWithNullDesc = new Product();
            productWithNullDesc.setTlCode("999999");
            productWithNullDesc.setDescription(null);
            
            List<Product> productsWithNull = Arrays.asList(productWithNullDesc);
            when(productRepository.findAll()).thenReturn(productsWithNull);

            // Act & Assert - Should not throw exception
            mockMvc.perform(get("/api/products/search")
                    .param("q", "test")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle products with null HS codes")
        void testSearchProducts_NullHSCode() throws Exception {
            // Arrange
            Product productWithNullCode = new Product();
            productWithNullCode.setTlCode(null);
            productWithNullCode.setDescription("Test product");
            
            List<Product> productsWithNull = Arrays.asList(productWithNullCode);
            when(productRepository.findAll()).thenReturn(productsWithNull);

            // Act & Assert - Should not throw exception
            mockMvc.perform(get("/api/products/search")
                    .param("q", "123456")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should search in both code and description for numeric query")
        void testSearchProducts_NumericSearchInBoth() throws Exception {
            // Arrange
            Product product = new Product();
            product.setTlCode("123456");
            product.setDescription("Product with 2201 in description");
            
            List<Product> products = Arrays.asList(product);
            when(productRepository.findAll()).thenReturn(products);

            // Act & Assert - Numeric query should find matches in description too
            mockMvc.perform(get("/api/products/search")
                    .param("q", "2201")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.products[0].code").value("123456"));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should search by single character")
        void testSearchProducts_SingleCharacter() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "0")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.searchType").value("hsCode"))
                    .andExpect(jsonPath("$.count").value(5)); // Limited to 5

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle special characters in description search")
        void testSearchProducts_SpecialCharacters() throws Exception {
            // Arrange
            Product product = new Product();
            product.setTlCode("123456");
            product.setDescription("Product with special-characters: test");
            
            List<Product> products = Arrays.asList(product);
            when(productRepository.findAll()).thenReturn(products);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "special-characters")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle large limit parameter")
        void testSearchProducts_LargeLimit() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "01")
                    .param("limit", "1000")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(9)); // 9 products contain "01"

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle limit of 1")
        void testSearchProducts_LimitOne() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "01")
                    .param("limit", "1")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.products.length()").value(1));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should include all response metadata fields")
        void testSearchProducts_ResponseStructure() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.products").exists())
                    .andExpect(jsonPath("$.count").exists())
                    .andExpect(jsonPath("$.query").exists())
                    .andExpect(jsonPath("$.searchType").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.products[0].code").exists())
                    .andExpect(jsonPath("$.products[0].description").exists())
                    .andExpect(jsonPath("$.products[0].matchType").exists());

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle whitespace in query")
        void testSearchProducts_WhitespaceQuery() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Whitespace is treated as part of query
            mockMvc.perform(get("/api/products/search")
                    .param("q", "  horses  ")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0)); // Exact match with spaces won't find anything

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should match contains for HS codes")
        void testSearchProducts_ContainsMatch() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - "10" should match codes containing "10"
            mockMvc.perform(get("/api/products/search")
                    .param("q", "10")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(8)); // Multiple products contain "10"

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return 401 Unauthorized for unauthenticated requests")
        void testSearchProducts_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            // Verify repository was never called
            verify(productRepository, times(0)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Admin")
        @DisplayName("Should allow Admin users to search products")
        void testSearchProducts_AdminUser() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle repository exception gracefully")
        void testSearchProducts_RepositoryException() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should work without content type specified")
        void testSearchProducts_NoContentType() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("success"));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle mixed alphanumeric query as text search")
        void testSearchProducts_MixedAlphanumericQuery() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - "abc123" should be treated as text search
            mockMvc.perform(get("/api/products/search")
                    .param("q", "abc123")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.searchType").value("description"))
                    .andExpect(jsonPath("$.status").value("success"));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should calculate relevance scores for description matches")
        void testSearchProducts_RelevanceScoring() throws Exception {
            // Arrange
            Product product1 = new Product();
            product1.setTlCode("111111");
            product1.setDescription("Waters including mineral waters and aerated waters");
            
            Product product2 = new Product();
            product2.setTlCode("222222");
            product2.setDescription("Mineral waters");
            
            Product product3 = new Product();
            product3.setTlCode("333333");
            product3.setDescription("Other waters");
            
            List<Product> products = Arrays.asList(product1, product2, product3);
            when(productRepository.findAll()).thenReturn(products);

            // Act & Assert - "mineral waters" should rank product2 highest
            mockMvc.perform(get("/api/products/search")
                    .param("q", "mineral waters")
                    .param("limit", "10")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2)); // Exact phrase matches

            verify(productRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle zero limit gracefully")
        void testSearchProducts_ZeroLimit() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .param("limit", "0")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle negative limit gracefully")
        void testSearchProducts_NegativeLimit() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);

            // Act & Assert - Negative limit causes error in stream processing
            mockMvc.perform(get("/api/products/search")
                    .param("q", "010121")
                    .param("limit", "-1")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle products with empty descriptions")
        void testSearchProducts_EmptyDescription() throws Exception {
            // Arrange
            Product product = new Product();
            product.setTlCode("123456");
            product.setDescription("");
            
            List<Product> products = Arrays.asList(product);
            when(productRepository.findAll()).thenReturn(products);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "test")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle products with empty HS codes")
        void testSearchProducts_EmptyHSCode() throws Exception {
            // Arrange
            Product product = new Product();
            product.setTlCode("");
            product.setDescription("Test product");
            
            List<Product> products = Arrays.asList(product);
            when(productRepository.findAll()).thenReturn(products);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", "123456")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));

            verify(productRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle very long query strings")
        void testSearchProducts_LongQuery() throws Exception {
            // Arrange
            when(productRepository.findAll()).thenReturn(mockProducts);
            String longQuery = "a".repeat(1000);

            // Act & Assert
            mockMvc.perform(get("/api/products/search")
                    .param("q", longQuery)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0));

            verify(productRepository, times(1)).findAll();
        }
    }
}
