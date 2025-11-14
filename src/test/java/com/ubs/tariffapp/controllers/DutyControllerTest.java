package com.ubs.tariffapp.controllers;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubs.tariffapp.exceptions.TariffNotFoundException;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.ExchangeRates;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.models.request.TariffCalculationRequest;
import com.ubs.tariffapp.models.request.TariffSearchRequest;
import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.services.ExchangeRateService;
import com.ubs.tariffapp.services.TariffScheduleService;

/**
 * Unit tests for DutyController.
 * Tests the tariff search and calculation endpoints with various duty types and scenarios.
 * Uses @WebMvcTest for lightweight testing with mocked dependencies (no database required).
 */
@WebMvcTest(DutyController.class)
@DisplayName("DutyController Unit Tests")
class DutyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DutyService dutyService;

    @MockitoBean
    private TariffScheduleService tariffScheduleService;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    private TariffSchedule createMockTariffSchedule(int id, String dutyTypeDesc, String tlCode) {
        TariffSchedule tariff = new TariffSchedule();
        tariff.setTariffId(id);
        tariff.setTariffYear(2024);
        
        DutyType dutyType = new DutyType();
        dutyType.setDutyTypeDescription(dutyTypeDesc);
        tariff.setDutyType(dutyType);
        
        Product product = new Product();
        product.setTlCode(tlCode);
        product.setDescription("Test Product");
        tariff.setProduct(product);
        
        tariff.setTlsSuffix("00");
        
        return tariff;
    }

    private AdValoremDuty createAdValoremDuty(double rate) {
        AdValoremDuty duty = new AdValoremDuty();
        duty.setRatePercent(BigDecimal.valueOf(rate));
        return duty;
    }

    private SpecificDuty createSpecificDuty(double amount, double multiplier, String unit) {
        SpecificDuty duty = new SpecificDuty();
        duty.setAmount(BigDecimal.valueOf(amount));
        duty.setMultiplier(Integer.valueOf((int) multiplier));
        duty.setUnit(unit);
        duty.setSpecificDutyRateRaw("$" + amount + " per " + unit);
        return duty;
    }

    private CombinedDuty createCombinedDuty(double rate, double amount, double multiplier, String unit, String type) {
        CombinedDuty duty = new CombinedDuty();
        duty.setRatePercent(BigDecimal.valueOf(rate));
        duty.setAmount(BigDecimal.valueOf(amount));
        duty.setMultiplier(Integer.valueOf((int) multiplier));
        duty.setUnit(unit);
        duty.setMixedOrCompound(type);
        duty.setSpecificDutyRateRaw("Combined: " + rate + "% + $" + amount + " per " + unit);
        return duty;
    }

    @Nested
    @DisplayName("POST /api/tariffs/search - Search Tariffs")
    class SearchTariffsTests {

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should search tariffs successfully with exact partner match")
        void testSearchTariffs_ExactPartnerMatch() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "CHN", "010121", 2024);
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Standard (MFN)", "010121");
            tariff.setDuty(createAdValoremDuty(5.0));
            
            List<TariffSchedule> tariffs = Arrays.asList(tariff);
            
            when(dutyService.searchAvailableTariffs("USA", "CHN", "010121", 2024))
                    .thenReturn(tariffs);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.year").value(2024))
                    .andExpect(jsonPath("$.usedFallback").value(false))
                    .andExpect(jsonPath("$.effectivePartnerCode").value("CHN"))
                    .andExpect(jsonPath("$.tariffs[0].tariffId").value(1))
                    .andExpect(jsonPath("$.tariffs[0].productCode").value("010121"))
                    .andExpect(jsonPath("$.tariffs[0].dutyClass").value("AdValoremDuty"));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should fallback to World (000) when partner not found")
        void testSearchTariffs_FallbackToWorld() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "XYZ", "010121", 2024);
            
            TariffSchedule tariff = createMockTariffSchedule(2, "Standard (MFN)", "010121");
            tariff.setDuty(createAdValoremDuty(10.0));
            
            List<TariffSchedule> worldTariffs = Arrays.asList(tariff);
            
            // First call with XYZ fails
            when(dutyService.searchAvailableTariffs("USA", "XYZ", "010121", 2024))
                    .thenThrow(new TariffNotFoundException("No tariffs found"));
            
            // Second call with World (000) succeeds
            when(dutyService.searchAvailableTariffs("USA", "000", "010121", 2024))
                    .thenReturn(worldTariffs);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(1))
                    .andExpect(jsonPath("$.usedFallback").value(true))
                    .andExpect(jsonPath("$.effectivePartnerCode").value("000"))
                    .andExpect(jsonPath("$.message").value("No tariffs found for specified partner. Showing World (000) tariffs."))
                    .andExpect(jsonPath("$.tariffs[0].tariffId").value(2));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return empty list when no tariffs found even for World")
        void testSearchTariffs_NoTariffsFound() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "XYZ", "999999", 2024);
            
            when(dutyService.searchAvailableTariffs("USA", "XYZ", "999999", 2024))
                    .thenThrow(new TariffNotFoundException("No tariffs found"));
            
            when(dutyService.searchAvailableTariffs("USA", "000", "999999", 2024))
                    .thenThrow(new TariffNotFoundException("No tariffs found"));

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.tariffs").isEmpty());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should not fallback when partner is already World (000)")
        void testSearchTariffs_AlreadyWorldPartner() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "000", "010121", 2024);
            
            when(dutyService.searchAvailableTariffs("USA", "000", "010121", 2024))
                    .thenThrow(new TariffNotFoundException("No tariffs found"));

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.tariffs").isEmpty())
                    .andExpect(jsonPath("$.usedFallback").value(false));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should include duty unit information for SpecificDuty")
        void testSearchTariffs_SpecificDutyWithUnit() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "CHN", "010121", 2024);
            
            TariffSchedule tariff = createMockTariffSchedule(3, "Specific", "010121");
            SpecificDuty duty = createSpecificDuty(2.5, 100.0, "kg");
            tariff.setDuty(duty);
            
            List<TariffSchedule> tariffs = Arrays.asList(tariff);
            
            when(dutyService.searchAvailableTariffs("USA", "CHN", "010121", 2024))
                    .thenReturn(tariffs);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffs[0].dutyClass").value("SpecificDuty"))
                    .andExpect(jsonPath("$.tariffs[0].unit").value("kg"));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should include duty unit information for CombinedDuty")
        void testSearchTariffs_CombinedDutyWithUnit() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "CHN", "010121", 2024);
            
            TariffSchedule tariff = createMockTariffSchedule(4, "Combined", "010121");
            CombinedDuty duty = createCombinedDuty(5.0, 2.5, 100.0, "liter", "C");
            tariff.setDuty(duty);
            
            List<TariffSchedule> tariffs = Arrays.asList(tariff);
            
            when(dutyService.searchAvailableTariffs("USA", "CHN", "010121", 2024))
                    .thenReturn(tariffs);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffs[0].dutyClass").value("CombinedDuty"))
                    .andExpect(jsonPath("$.tariffs[0].unit").value("liter"));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return multiple tariffs when available")
        void testSearchTariffs_MultipleTariffs() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "CHN", "010121", 2024);
            
            TariffSchedule tariff1 = createMockTariffSchedule(1, "Standard (MFN)", "010121");
            tariff1.setDuty(createAdValoremDuty(5.0));
            tariff1.setTlsSuffix("00");
            
            TariffSchedule tariff2 = createMockTariffSchedule(2, "Standard (MFN)", "010121");
            tariff2.setDuty(createAdValoremDuty(3.0));
            tariff2.setTlsSuffix("10");
            
            List<TariffSchedule> tariffs = Arrays.asList(tariff1, tariff2);
            
            when(dutyService.searchAvailableTariffs("USA", "CHN", "010121", 2024))
                    .thenReturn(tariffs);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.tariffs[0].tariffId").value(1))
                    .andExpect(jsonPath("$.tariffs[0].tlsSuffix").value("00"))
                    .andExpect(jsonPath("$.tariffs[1].tariffId").value(2))
                    .andExpect(jsonPath("$.tariffs[1].tlsSuffix").value("10"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized for unauthenticated requests")
        void testSearchTariffs_Unauthorized() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "CHN", "010121", 2024);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/tariffs/calculate - Calculate Tariff")
    class CalculateTariffTests {

        private ExchangeRates mockExchangeRates;

        @BeforeEach
        void setUp() {
            // Setup mock exchange rates
            mockExchangeRates = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            rates.put("EUR", 0.85);
            rates.put("GBP", 0.73);
            rates.put("JPY", 110.0);
            rates.put("SGD", 1.35);
            mockExchangeRates.setRates(rates);
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should calculate AdValorem duty in USD")
        void testCalculateTariff_AdValoremUSD() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(1);
            request.setAmountOfProduct(1000.0);
            request.setCurrency("USD");
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Ad Valorem", "010121");
            AdValoremDuty duty = createAdValoremDuty(5.0);
            tariff.setDuty(duty);
            
            when(dutyService.calculateTariffById(1, 1000.0)).thenReturn(50.0);
            when(tariffScheduleService.getTariffScheduleById(1)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.tariffId").value(1))
                    .andExpect(jsonPath("$.tariffAmount").value(50.0))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.dutyType").value("Ad Valorem"))
                    .andExpect(jsonPath("$.dutyTypeCode").value("AD_VALOREM"))
                    .andExpect(jsonPath("$.rate").value(5.0))
                    .andExpect(jsonPath("$.productValue").value(1000.0))
                    .andExpect(jsonPath("$.formula").exists())
                    .andExpect(jsonPath("$.steps").isArray());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should calculate AdValorem duty with currency conversion")
        void testCalculateTariff_AdValoremWithConversion() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(1);
            request.setAmountOfProduct(1000.0);
            request.setCurrency("EUR");
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Ad Valorem", "010121");
            AdValoremDuty duty = createAdValoremDuty(5.0);
            tariff.setDuty(duty);
            
            when(dutyService.calculateTariffById(1, 1000.0)).thenReturn(50.0);
            when(tariffScheduleService.getTariffScheduleById(1)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - Should convert 50 USD to EUR (50 * 0.85 = 42.5)
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffAmount").value(42.5))
                    .andExpect(jsonPath("$.currency").value("EUR"));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should default to USD when currency not specified")
        void testCalculateTariff_DefaultCurrency() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(1);
            request.setAmountOfProduct(1000.0);
            // No currency set
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Ad Valorem", "010121");
            tariff.setDuty(createAdValoremDuty(5.0));
            
            when(dutyService.calculateTariffById(1, 1000.0)).thenReturn(50.0);
            when(tariffScheduleService.getTariffScheduleById(1)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.tariffAmount").value(50.0));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should calculate SpecificDuty with detailed steps")
        void testCalculateTariff_SpecificDuty() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(2);
            request.setAmountOfProduct(500.0); // kg
            request.setCurrency("USD");
            
            TariffSchedule tariff = createMockTariffSchedule(2, "Specific", "010121");
            SpecificDuty duty = createSpecificDuty(2.5, 100.0, "kg");
            tariff.setDuty(duty);
            
            when(dutyService.calculateTariffById(2, 500.0)).thenReturn(12.5);
            when(tariffScheduleService.getTariffScheduleById(2)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dutyTypeCode").value("SPECIFIC"))
                    .andExpect(jsonPath("$.tariffAmount").value(12.5))
                    .andExpect(jsonPath("$.amountPerUnit").value(2.5))
                    .andExpect(jsonPath("$.multiplier").value(100.0))
                    .andExpect(jsonPath("$.calculatedUnits").value(5.0))
                    .andExpect(jsonPath("$.formula").exists())
                    .andExpect(jsonPath("$.steps").isArray())
                    .andExpect(jsonPath("$.steps[0].description").value("Product Quantity (in kg)"));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should calculate CombinedDuty (Compound) with separate values")
        void testCalculateTariff_CombinedDutyCompound_SeparateValues() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(3);
            request.setAmountOfProduct(500.0); // quantity for specific
            request.setProductValueDollars(10000.0); // value for ad valorem
            request.setCurrency("USD");
            
            TariffSchedule tariff = createMockTariffSchedule(3, "Combined", "010121");
            CombinedDuty duty = createCombinedDuty(5.0, 2.5, 100.0, "liter", "C");
            tariff.setDuty(duty);
            
            // Combined Compound: Ad Valorem (10000 * 0.05) + Specific ((500/100) * 2.5) = 500 + 12.5 = 512.5
            when(dutyService.calculateTariffById(3, 500.0, 10000.0)).thenReturn(512.5);
            when(tariffScheduleService.getTariffScheduleById(3)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dutyTypeCode").value("COMBINED"))
                    .andExpect(jsonPath("$.combinationType").value("Compound (Sum of both)"))
                    .andExpect(jsonPath("$.tariffAmount").value(512.5))
                    .andExpect(jsonPath("$.productValueDollars").value(10000.0))
                    .andExpect(jsonPath("$.productQuantity").value(500.0))
                    .andExpect(jsonPath("$.adValoremAmount").value(500.0))
                    .andExpect(jsonPath("$.specificAmount").value(12.5))
                    .andExpect(jsonPath("$.formula").exists())
                    .andExpect(jsonPath("$.steps").isArray());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should calculate CombinedDuty (Mixed) with separate values")
        void testCalculateTariff_CombinedDutyMixed_SeparateValues() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(4);
            request.setAmountOfProduct(500.0);
            request.setProductValueDollars(10000.0);
            request.setCurrency("USD");
            
            TariffSchedule tariff = createMockTariffSchedule(4, "Combined", "010121");
            CombinedDuty duty = createCombinedDuty(5.0, 2.5, 100.0, "liter", "M");
            tariff.setDuty(duty);
            
            // Combined Mixed: MAX(500, 12.5) = 500
            when(dutyService.calculateTariffById(4, 500.0, 10000.0)).thenReturn(500.0);
            when(tariffScheduleService.getTariffScheduleById(4)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dutyTypeCode").value("COMBINED"))
                    .andExpect(jsonPath("$.combinationType").value("Mixed (Maximum of both)"))
                    .andExpect(jsonPath("$.tariffAmount").value(500.0))
                    .andExpect(jsonPath("$.formula").exists());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should calculate CombinedDuty (Compound) with single value (legacy)")
        void testCalculateTariff_CombinedDutyCompound_SingleValue() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(5);
            request.setAmountOfProduct(1000.0);
            request.setCurrency("USD");
            // No productValueDollars - legacy mode
            
            TariffSchedule tariff = createMockTariffSchedule(5, "Combined", "010121");
            CombinedDuty duty = createCombinedDuty(5.0, 2.5, 100.0, "liter", "C");
            tariff.setDuty(duty);
            
            when(dutyService.calculateTariffById(5, 1000.0)).thenReturn(75.0);
            when(tariffScheduleService.getTariffScheduleById(5)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dutyTypeCode").value("COMBINED"))
                    .andExpect(jsonPath("$.tariffAmount").value(75.0))
                    .andExpect(jsonPath("$.productValue").value(1000.0))
                    .andExpect(jsonPath("$.productValueDollars").doesNotExist());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle missing exchange rates gracefully")
        void testCalculateTariff_MissingExchangeRates() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(1);
            request.setAmountOfProduct(1000.0);
            request.setCurrency("EUR");
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Ad Valorem", "010121");
            tariff.setDuty(createAdValoremDuty(5.0));
            
            when(dutyService.calculateTariffById(1, 1000.0)).thenReturn(50.0);
            when(tariffScheduleService.getTariffScheduleById(1)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(null);

            // Act & Assert - Should use rate of 1.0 when rates unavailable
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffAmount").value(50.0))
                    .andExpect(jsonPath("$.currency").value("EUR"));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle unsupported currency gracefully")
        void testCalculateTariff_UnsupportedCurrency() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(1);
            request.setAmountOfProduct(1000.0);
            request.setCurrency("XYZ");
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Ad Valorem", "010121");
            tariff.setDuty(createAdValoremDuty(5.0));
            
            when(dutyService.calculateTariffById(1, 1000.0)).thenReturn(50.0);
            when(tariffScheduleService.getTariffScheduleById(1)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - Should use rate of 1.0 for unsupported currency
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffAmount").value(50.0))
                    .andExpect(jsonPath("$.currency").value("XYZ"));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should include calculation steps in response")
        void testCalculateTariff_IncludesCalculationSteps() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(1);
            request.setAmountOfProduct(1000.0);
            request.setCurrency("USD");
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Ad Valorem", "010121");
            tariff.setDuty(createAdValoremDuty(5.0));
            
            when(dutyService.calculateTariffById(1, 1000.0)).thenReturn(50.0);
            when(tariffScheduleService.getTariffScheduleById(1)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate") 
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.steps").isArray())
                    .andExpect(jsonPath("$.steps[0].step").exists())
                    .andExpect(jsonPath("$.steps[0].description").exists())
                    .andExpect(jsonPath("$.steps[0].value").exists())
                    .andExpect(jsonPath("$.calculation").exists())
                    .andExpect(jsonPath("$.formula").exists());
        }

        @Test
        @DisplayName("Should return 401 Unauthorized for unauthenticated requests")
        void testCalculateTariff_Unauthorized() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(1);
            request.setAmountOfProduct(1000.0);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle calculation service errors")
        void testCalculateTariff_ServiceError() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(999);
            request.setAmountOfProduct(1000.0);
            
            when(dutyService.calculateTariffById(999, 1000.0))
                    .thenThrow(new TariffNotFoundException("Tariff not found"));

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle CombinedDuty with null unit (Compound)")
        void testCalculateTariff_CombinedDutyCompound_NullUnit() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(10);
            request.setAmountOfProduct(500.0);
            request.setProductValueDollars(10000.0);
            request.setCurrency("USD");
            
            TariffSchedule tariff = createMockTariffSchedule(10, "Combined", "010121");
            CombinedDuty duty = createCombinedDuty(5.0, 2.5, 100.0, null, "C");
            tariff.setDuty(duty);
            
            when(dutyService.calculateTariffById(10, 500.0, 10000.0)).thenReturn(512.5);
            when(tariffScheduleService.getTariffScheduleById(10)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - Should handle null unit gracefully
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dutyTypeCode").value("COMBINED"))
                    .andExpect(jsonPath("$.tariffAmount").value(512.5))
                    .andExpect(jsonPath("$.formula").exists())
                    .andExpect(jsonPath("$.steps").isArray());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle CombinedDuty with null unit (Mixed)")
        void testCalculateTariff_CombinedDutyMixed_NullUnit() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(11);
            request.setAmountOfProduct(500.0);
            request.setProductValueDollars(10000.0);
            request.setCurrency("USD");
            
            TariffSchedule tariff = createMockTariffSchedule(11, "Combined", "010121");
            CombinedDuty duty = createCombinedDuty(5.0, 2.5, 100.0, null, "M");
            tariff.setDuty(duty);
            
            when(dutyService.calculateTariffById(11, 500.0, 10000.0)).thenReturn(500.0);
            when(tariffScheduleService.getTariffScheduleById(11)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - Should handle null unit gracefully
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dutyTypeCode").value("COMBINED"))
                    .andExpect(jsonPath("$.tariffAmount").value(500.0))
                    .andExpect(jsonPath("$.formula").exists())
                    .andExpect(jsonPath("$.steps").isArray());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle CombinedDuty with null unit (Legacy mode)")
        void testCalculateTariff_CombinedDutyLegacy_NullUnit() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(12);
            request.setAmountOfProduct(1000.0);
            request.setCurrency("USD");
            // No productValueDollars - legacy mode
            
            TariffSchedule tariff = createMockTariffSchedule(12, "Combined", "010121");
            CombinedDuty duty = createCombinedDuty(5.0, 2.5, 100.0, null, "M");
            tariff.setDuty(duty);
            
            when(dutyService.calculateTariffById(12, 1000.0)).thenReturn(50.0);
            when(tariffScheduleService.getTariffScheduleById(12)).thenReturn(tariff);
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - Should handle null unit in legacy mode
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffAmount").value(50.0))
                    .andExpect(jsonPath("$.formula").value("Tariff = MAX(Ad Valorem, Specific Duty) (NOTE: Input has mixed semantics)"))
                    .andExpect(jsonPath("$.steps").isArray());
        }
    }

    @Nested
    @DisplayName("Helper Methods Tests")
    class HelperMethodsTests {

        private ExchangeRates mockExchangeRates;

        @BeforeEach
        void setUp() {
            mockExchangeRates = new ExchangeRates();
            Map<String, Double> rates = new HashMap<>();
            rates.put("EUR", 0.85);
            mockExchangeRates.setRates(rates);
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should build tariff description correctly")
        void testBuildTariffDescription() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "CHN", "010121", 2024);
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Standard (MFN)", "010121");
            tariff.setDuty(createAdValoremDuty(5.0));
            tariff.setTlsSuffix("AB");
            
            List<TariffSchedule> tariffs = Arrays.asList(tariff);
            
            when(dutyService.searchAvailableTariffs(anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(tariffs);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffs[0].description").value("Test Product (AB) - Standard (MFN)"));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle null duty gracefully in search")
        void testSearchTariffs_NullDuty() throws Exception {
            // Arrange
            TariffSearchRequest request = new TariffSearchRequest("USA", "CHN", "010121", 2024);
            
            TariffSchedule tariff = createMockTariffSchedule(1, "Standard", "010121");
            // No duty set
            
            List<TariffSchedule> tariffs = Arrays.asList(tariff);
            
            when(dutyService.searchAvailableTariffs(anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(tariffs);

            // Act & Assert
            mockMvc.perform(post("/api/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffs[0].tariffId").value(1))
                    .andExpect(jsonPath("$.tariffs[0].dutyClass").doesNotExist());
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should handle error in calculation details gracefully")
        void testCalculateTariff_DetailsError() throws Exception {
            // Arrange
            TariffCalculationRequest request = new TariffCalculationRequest();
            request.setTariffId(1);
            request.setAmountOfProduct(1000.0);
            request.setCurrency("USD");
            
            when(dutyService.calculateTariffById(1, 1000.0)).thenReturn(50.0);
            when(tariffScheduleService.getTariffScheduleById(1))
                    .thenThrow(new RuntimeException("Database error"));
            when(exchangeRateService.fetchRates()).thenReturn(mockExchangeRates);

            // Act & Assert - Should still return tariff amount even if details fail
            mockMvc.perform(post("/api/tariffs/calculate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffAmount").value(50.0))
                    .andExpect(jsonPath("$.error").value("Could not retrieve calculation details"));
        }
    }
}
