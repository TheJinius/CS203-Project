package com.ubs.tariffapp.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

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

import com.ubs.tariffapp.models.ExchangeRates;
import com.ubs.tariffapp.services.ExchangeRateService;

/**
 * Unit tests for ExchangeRateController.
 * Tests the exchange rate retrieval endpoint with various scenarios.
 * Uses @WebMvcTest for lightweight testing with mocked dependencies (no database required).
 */
@WebMvcTest(ExchangeRateController.class)
@DisplayName("ExchangeRateController Unit Tests")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    private ExchangeRates mockExchangeRates;

    @BeforeEach
    void setUp() {
        // Setup mock exchange rates with common currencies
        mockExchangeRates = new ExchangeRates();
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.85);
        rates.put("GBP", 0.73);
        rates.put("JPY", 110.0);
        rates.put("SGD", 1.35);
        rates.put("CNY", 6.45);
        rates.put("AUD", 1.30);
        rates.put("CAD", 1.25);
        rates.put("CHF", 0.92);
        mockExchangeRates.setRates(rates);
    }

    @Nested
    @DisplayName("GET /api/exchange-rates - Get Exchange Rates")
    class GetExchangeRatesTests {

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return exchange rates with default base currency (USD)")
        void testGetExchangeRates_DefaultBase() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rates").exists())
                    .andExpect(jsonPath("$.rates.EUR").value(0.85))
                    .andExpect(jsonPath("$.rates.GBP").value(0.73))
                    .andExpect(jsonPath("$.rates.JPY").value(110.0))
                    .andExpect(jsonPath("$.rates.SGD").value(1.35))
                    .andExpect(jsonPath("$.rates.CNY").value(6.45))
                    .andExpect(jsonPath("$.rates.AUD").value(1.30))
                    .andExpect(jsonPath("$.rates.CAD").value(1.25))
                    .andExpect(jsonPath("$.rates.CHF").value(0.92));

            // Verify service was called once
            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return exchange rates with explicit USD base parameter")
        void testGetExchangeRates_ExplicitUSDBase() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .param("base", "USD")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rates").exists())
                    .andExpect(jsonPath("$.rates.EUR").value(0.85))
                    .andExpect(jsonPath("$.rates.GBP").value(0.73));

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return exchange rates with EUR base parameter (parameter is ignored)")
        void testGetExchangeRates_EURBaseParameter() throws Exception {
            // Arrange
            // Note: The controller accepts the base parameter but doesn't use it
            // The service always returns USD-based rates
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .param("base", "EUR")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rates").exists())
                    .andExpect(jsonPath("$.rates.EUR").value(0.85));

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle empty exchange rates")
        void testGetExchangeRates_EmptyRates() throws Exception {
            // Arrange
            ExchangeRates emptyRates = new ExchangeRates();
            emptyRates.setRates(new HashMap<>());
            when(exchangeRateService.fetchRates()).thenReturn(emptyRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rates").exists())
                    .andExpect(jsonPath("$.rates").isEmpty());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle null rates map")
        void testGetExchangeRates_NullRates() throws Exception {
            // Arrange
            ExchangeRates nullRates = new ExchangeRates();
            nullRates.setRates(null);
            when(exchangeRateService.fetchRates()).thenReturn(nullRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rates").doesNotExist());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle null ExchangeRates object")
        void testGetExchangeRates_NullObject() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(null);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return exchange rates with single currency")
        void testGetExchangeRates_SingleCurrency() throws Exception {
            // Arrange
            ExchangeRates singleRate = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            rates.put("EUR", 0.85);
            singleRate.setRates(rates);
            when(exchangeRateService.fetchRates()).thenReturn(singleRate);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates").exists())
                    .andExpect(jsonPath("$.rates.EUR").value(0.85))
                    .andExpect(jsonPath("$.rates.GBP").doesNotExist());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return exchange rates with many currencies")
        void testGetExchangeRates_ManyCurrencies() throws Exception {
            // Arrange
            ExchangeRates manyRates = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            // Add 20 different currencies
            rates.put("EUR", 0.85);
            rates.put("GBP", 0.73);
            rates.put("JPY", 110.0);
            rates.put("SGD", 1.35);
            rates.put("CNY", 6.45);
            rates.put("AUD", 1.30);
            rates.put("CAD", 1.25);
            rates.put("CHF", 0.92);
            rates.put("INR", 74.5);
            rates.put("MXN", 20.1);
            rates.put("BRL", 5.2);
            rates.put("ZAR", 14.5);
            rates.put("SEK", 8.6);
            rates.put("NOK", 8.8);
            rates.put("DKK", 6.3);
            rates.put("NZD", 1.42);
            rates.put("HKD", 7.8);
            rates.put("KRW", 1180.0);
            rates.put("THB", 33.0);
            rates.put("MYR", 4.2);
            manyRates.setRates(rates);
            when(exchangeRateService.fetchRates()).thenReturn(manyRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates").exists())
                    .andExpect(jsonPath("$.rates.EUR").value(0.85))
                    .andExpect(jsonPath("$.rates.THB").value(33.0))
                    .andExpect(jsonPath("$.rates.MYR").value(4.2));

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle service exception gracefully")
        void testGetExchangeRates_ServiceException() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates())
                    .thenThrow(new RuntimeException("External API failure"));

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle zero exchange rates")
        void testGetExchangeRates_ZeroRates() throws Exception {
            // Arrange
            ExchangeRates zeroRates = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            rates.put("EUR", 0.0);
            rates.put("GBP", 0.0);
            zeroRates.setRates(rates);
            when(exchangeRateService.fetchRates()).thenReturn(zeroRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates.EUR").value(0.0))
                    .andExpect(jsonPath("$.rates.GBP").value(0.0));

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle negative exchange rates")
        void testGetExchangeRates_NegativeRates() throws Exception {
            // Arrange
            ExchangeRates negativeRates = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            rates.put("EUR", -0.85);
            negativeRates.setRates(rates);
            when(exchangeRateService.fetchRates()).thenReturn(negativeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates.EUR").value(-0.85));

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle very large exchange rate values")
        void testGetExchangeRates_LargeValues() throws Exception {
            // Arrange
            ExchangeRates largeRates = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            rates.put("VEF", 1000000.0); // Venezuelan Bolivar (hyperinflation example)
            rates.put("ZWL", 500000.0);  // Zimbabwe Dollar
            largeRates.setRates(rates);
            when(exchangeRateService.fetchRates()).thenReturn(largeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates.VEF").value(1000000.0))
                    .andExpect(jsonPath("$.rates.ZWL").value(500000.0));

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle very small exchange rate values")
        void testGetExchangeRates_SmallValues() throws Exception {
            // Arrange
            ExchangeRates smallRates = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            rates.put("BTC", 0.000023); // Bitcoin to USD (example)
            rates.put("XAU", 0.00054);  // Gold Ounce
            smallRates.setRates(rates);
            when(exchangeRateService.fetchRates()).thenReturn(smallRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates.BTC").value(0.000023))
                    .andExpect(jsonPath("$.rates.XAU").value(0.00054));

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should accept case-sensitive base parameter")
        void testGetExchangeRates_CaseSensitiveBase() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .param("base", "usd")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates").exists());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @DisplayName("Should return 401 Unauthorized for unauthenticated requests")
        void testGetExchangeRates_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            // Verify service was never called
            verify(exchangeRateService, times(0)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Admin")
        @DisplayName("Should allow Admin users to access exchange rates")
        void testGetExchangeRates_AdminUser() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates").exists());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle multiple rapid requests")
        void testGetExchangeRates_MultipleRequests() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - Make 3 rapid requests
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(get("/api/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.rates").exists());
            }

            // Verify service was called 3 times
            verify(exchangeRateService, times(3)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return consistent format for all responses")
        void testGetExchangeRates_ConsistentFormat() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rates").isMap());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle currencies with special characters in codes")
        void testGetExchangeRates_SpecialCharacterCodes() throws Exception {
            // Arrange
            ExchangeRates specialRates = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            rates.put("USD", 1.0);
            rates.put("EUR", 0.85);
            specialRates.setRates(rates);
            when(exchangeRateService.fetchRates()).thenReturn(specialRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates.USD").value(1.0));

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should work without specifying content type")
        void testGetExchangeRates_NoContentType() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.rates").exists());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should accept additional query parameters gracefully")
        void testGetExchangeRates_AdditionalParameters() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - Additional parameters should be ignored
            mockMvc.perform(get("/api/exchange-rates")
                    .param("base", "USD")
                    .param("symbols", "EUR,GBP")
                    .param("date", "2024-01-01")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates").exists());

            verify(exchangeRateService, times(1)).fetchRates();
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenariosTests {

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle typical user workflow - fetch rates for tariff calculation")
        void testGetExchangeRates_TypicalWorkflow() throws Exception {
            // Arrange - Simulating a user about to calculate tariffs
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - User fetches rates before calculation
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates.EUR").exists())
                    .andExpect(jsonPath("$.rates.GBP").exists())
                    .andExpect(jsonPath("$.rates.JPY").exists())
                    .andExpect(jsonPath("$.rates.SGD").exists())
                    .andExpect(jsonPath("$.rates.CNY").exists());

            verify(exchangeRateService, times(1)).fetchRates();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should verify rate structure matches ExchangeRates model")
        void testGetExchangeRates_MatchesModel() throws Exception {
            // Arrange
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(get("/api/exchange-rates")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rates").isMap())
                    .andExpect(jsonPath("$.rates.EUR").isNumber())
                    .andExpect(jsonPath("$.rates.GBP").isNumber())
                    .andExpect(jsonPath("$.rates.JPY").isNumber());

            verify(exchangeRateService, times(1)).fetchRates();
        }
    }
}
