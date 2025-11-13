package com.ubs.tariffapp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.dto.TariffRequest;
import com.ubs.tariffapp.models.dto.TariffResponse;
import com.ubs.tariffapp.models.request.TariffSearchRequest;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.services.TariffManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for TariffManagementController
 * Tests all endpoints with various scenarios including success, failure, and error cases
 */
@WebMvcTest(TariffManagementController.class)
@DisplayName("TariffManagementController Tests")
class TariffManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TariffManagementService tariffManagementService;

    @MockBean
    private DutyService dutyService;

    @MockBean
    private CountryRepository countryRepository;

    @MockBean
    private TariffScheduleRepository tariffScheduleRepository;

    @Nested
    @DisplayName("GET /api/admin/tariffs - Get All Tariffs")
    class GetAllTariffsTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return all tariffs for admin user")
        void testGetAllTariffs_Success() throws Exception {
            // Arrange
            List<TariffResponse> tariffs = Arrays.asList(
                createTariffResponse(1, 2024, "USA", "CHN", "010121"),
                createTariffResponse(2, 2024, "USA", "SGP", "010129")
            );
            when(tariffManagementService.getAllTariffs()).thenReturn(tariffs);

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.tariffs").isArray())
                    .andExpect(jsonPath("$.tariffs", hasSize(2)))
                    .andExpect(jsonPath("$.tariffs[0].tariffId").value(1))
                    .andExpect(jsonPath("$.tariffs[1].tariffId").value(2));

            verify(tariffManagementService, times(1)).getAllTariffs();
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return empty list when no tariffs exist")
        void testGetAllTariffs_EmptyList() throws Exception {
            // Arrange
            when(tariffManagementService.getAllTariffs()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(0))
                    .andExpect(jsonPath("$.tariffs").isArray())
                    .andExpect(jsonPath("$.tariffs", hasSize(0)));

            verify(tariffManagementService, times(1)).getAllTariffs();
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void testGetAllTariffs_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs"))
                    .andExpect(status().isUnauthorized());

            verify(tariffManagementService, never()).getAllTariffs();
        }
    }

    @Nested
    @DisplayName("POST /api/admin/tariffs/search - Search Tariffs")
    class SearchTariffsTests {

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should allow Users to search tariffs")
        void testSearchTariffs_UsersAccess() throws Exception {
            // Arrange
            TariffSearchRequest searchRequest = new TariffSearchRequest("USA", "CHN", "010121", 2024);
            when(dutyService.searchAvailableTariffs(anyString(), anyString(), anyString(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));

            verify(dutyService, times(1)).searchAvailableTariffs(anyString(), anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void testSearchTariffs_Unauthorized() throws Exception {
            // Arrange
            TariffSearchRequest searchRequest = new TariffSearchRequest("USA", "CHN", "010121", 2024);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs/search")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest)))
                    .andExpect(status().isUnauthorized());

            verify(dutyService, never()).searchAvailableTariffs(anyString(), anyString(), anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tariffs/{id} - Get Tariff by ID")
    class GetTariffByIdTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return tariff details by ID")
        void testGetTariffById_Success() throws Exception {
            // Arrange
            TariffResponse response = createTariffResponse(1, 2024, "USA", "CHN", "010121");
            when(tariffManagementService.getTariffById(1)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffId").value(1))
                    .andExpect(jsonPath("$.tariffYear").value(2024))
                    .andExpect(jsonPath("$.reporterCode").value("USA"));

            verify(tariffManagementService, times(1)).getTariffById(1);
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should handle service exception when tariff not found")
        void testGetTariffById_NotFound() throws Exception {
            // Arrange
            when(tariffManagementService.getTariffById(999))
                    .thenThrow(new RuntimeException("Tariff not found"));

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/999"))
                    .andExpect(status().isInternalServerError());

            verify(tariffManagementService, times(1)).getTariffById(999);
        }
    }

    @Nested
    @DisplayName("POST /api/admin/tariffs - Create Tariff")
    class CreateTariffTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should create new tariff successfully")
        void testCreateTariff_Success() throws Exception {
            // Arrange
            TariffRequest request = createTariffRequest();
            TariffResponse response = createTariffResponse(1, 2024, "USA", "CHN", "010121");
            when(tariffManagementService.createTariff(any(TariffRequest.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tariffId").value(1))
                    .andExpect(jsonPath("$.reporterCode").value("USA"));

            verify(tariffManagementService, times(1)).createTariff(any(TariffRequest.class));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 403 when CSRF token is missing")
        void testCreateTariff_MissingCsrf() throws Exception {
            // Arrange
            TariffRequest request = createTariffRequest();

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verify(tariffManagementService, never()).createTariff(any(TariffRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/tariffs/{id} - Update Tariff")
    class UpdateTariffTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should update tariff successfully")
        void testUpdateTariff_Success() throws Exception {
            // Arrange
            Map<String, Object> updates = new HashMap<>();
            updates.put("tlsSuffix", "A");
            updates.put("note", "Updated note");
            updates.put("adValoremRate", 10.5);
            
            TariffResponse response = createTariffResponse(1, 2024, "USA", "CHN", "010121");
            when(tariffManagementService.updateTariff(eq(1), any())).thenReturn(response);

            // Act & Assert
            mockMvc.perform(put("/api/admin/tariffs/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updates)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffId").value(1));

            verify(tariffManagementService, times(1)).updateTariff(eq(1), any());
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should update only tlsSuffix field")
        void testUpdateTariff_PartialUpdate() throws Exception {
            // Arrange
            Map<String, Object> updates = new HashMap<>();
            updates.put("tlsSuffix", "B");
            
            TariffResponse response = createTariffResponse(1, 2024, "USA", "CHN", "010121");
            when(tariffManagementService.updateTariff(eq(1), any())).thenReturn(response);

            // Act & Assert
            mockMvc.perform(put("/api/admin/tariffs/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updates)))
                    .andExpect(status().isOk());

            verify(tariffManagementService, times(1)).updateTariff(eq(1), any());
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should handle update for non-existent tariff")
        void testUpdateTariff_NotFound() throws Exception {
            // Arrange
            Map<String, Object> updates = new HashMap<>();
            updates.put("note", "Updated");
            when(tariffManagementService.updateTariff(eq(999), any()))
                    .thenThrow(new RuntimeException("Tariff not found"));

            // Act & Assert
            mockMvc.perform(put("/api/admin/tariffs/999")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updates)))
                    .andExpect(status().isInternalServerError());

            verify(tariffManagementService, times(1)).updateTariff(eq(999), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/tariffs/{id} - Delete Tariff")
    class DeleteTariffTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should delete tariff successfully")
        void testDeleteTariff_Success() throws Exception {
            // Arrange
            doNothing().when(tariffManagementService).deleteTariff(1);

            // Act & Assert
            mockMvc.perform(delete("/api/admin/tariffs/1")
                    .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(tariffManagementService, times(1)).deleteTariff(1);
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 403 when CSRF token is missing")
        void testDeleteTariff_MissingCsrf() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/admin/tariffs/1"))
                    .andExpect(status().isForbidden());

            verify(tariffManagementService, never()).deleteTariff(anyInt());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tariffs/countries - Get All Countries")
    class GetAllCountriesTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return all countries sorted alphabetically")
        void testGetAllCountries_Success() throws Exception {
            // Arrange
            List<Country> countries = Arrays.asList(
                createCountry("USA", "United States"),
                createCountry("CHN", "China"),
                createCountry("SGP", "Singapore")
            );
            when(countryRepository.findAll()).thenReturn(countries);

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/countries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(3))
                    .andExpect(jsonPath("$.countries").isArray())
                    .andExpect(jsonPath("$.countries", hasSize(3)))
                    // Verify alphabetical sorting
                    .andExpect(jsonPath("$.countries[0].name").value("China"))
                    .andExpect(jsonPath("$.countries[1].name").value("Singapore"))
                    .andExpect(jsonPath("$.countries[2].name").value("United States"));

            verify(countryRepository, times(1)).findAll();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should allow Users to access countries endpoint")
        void testGetAllCountries_UsersAccess() throws Exception {
            // Arrange
            when(countryRepository.findAll()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/countries"))
                    .andExpect(status().isOk());

            verify(countryRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void testGetAllCountries_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/countries"))
                    .andExpect(status().isUnauthorized());

            verify(countryRepository, never()).findAll();
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tariffs/countries/with-tariffs - Get Countries With Tariffs")
    class GetCountriesWithTariffsTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return countries that have tariffs")
        void testGetCountriesWithTariffs_Success() throws Exception {
            // Arrange
            List<Country> countries = Arrays.asList(
                createCountry("USA", "United States"),
                createCountry("CHN", "China")
            );
            when(tariffScheduleRepository.findDistinctCountries()).thenReturn(countries);

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/countries/with-tariffs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(2))
                    .andExpect(jsonPath("$.countries").isArray())
                    .andExpect(jsonPath("$.countries", hasSize(2)));

            verify(tariffScheduleRepository, times(1)).findDistinctCountries();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should allow Users to access countries with tariffs")
        void testGetCountriesWithTariffs_UsersAccess() throws Exception {
            // Arrange
            when(tariffScheduleRepository.findDistinctCountries()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/countries/with-tariffs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));

            verify(tariffScheduleRepository, times(1)).findDistinctCountries();
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tariffs/years - Get Available Years")
    class GetAvailableYearsTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return available tariff years")
        void testGetAvailableYears_Success() throws Exception {
            // Arrange
            List<Integer> years = Arrays.asList(2024, 2023, 2022);
            when(tariffScheduleRepository.findDistinctYears()).thenReturn(years);

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/years"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.count").value(3))
                    .andExpect(jsonPath("$.years").isArray())
                    .andExpect(jsonPath("$.years", hasSize(3)))
                    .andExpect(jsonPath("$.years[0]").value(2024))
                    .andExpect(jsonPath("$.years[1]").value(2023))
                    .andExpect(jsonPath("$.years[2]").value(2022));

            verify(tariffScheduleRepository, times(1)).findDistinctYears();
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should allow Users to access years endpoint")
        void testGetAvailableYears_UsersAccess() throws Exception {
            // Arrange
            when(tariffScheduleRepository.findDistinctYears()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/years"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(0));

            verify(tariffScheduleRepository, times(1)).findDistinctYears();
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void testGetAvailableYears_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/years"))
                    .andExpect(status().isUnauthorized());

            verify(tariffScheduleRepository, never()).findDistinctYears();
        }
    }

    // Helper methods to create test data
    private TariffResponse createTariffResponse(Integer id, Integer year, String reporterCode, 
                                               String partnerCode, String tlCode) {
        return TariffResponse.builder()
                .tariffId(id)
                .tariffYear(year)
                .reporterCode(reporterCode)
                .reporterName("Reporter Name")
                .partnerCode(partnerCode)
                .partnerName("Partner Name")
                .tlCode(tlCode)
                .productDescription("Product Description")
                .dutyType("0")
                .dutyCode("0")
                .dutyTypeDescription("Standard (MFN)")
                .dutyCategory("A")
                .adValoremRate(5.0)
                .build();
    }

    private TariffRequest createTariffRequest() {
        TariffRequest request = new TariffRequest();
        request.setTariffYear(2024);
        request.setReporterCode("USA");
        request.setPartnerCode("CHN");
        request.setTlCode("010121");
        request.setDutyType("0");
        request.setDutyCode("0");
        request.setAdValoremRate(5.0);
        return request;
    }

    private Country createCountry(String code, String name) {
        Country country = new Country();
        country.setCountryId(code);
        country.setCountryName(name);
        return country;
    }
}
