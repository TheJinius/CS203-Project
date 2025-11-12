package com.ubs.tariffapp.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubs.tariffapp.exceptions.InvalidRequestException;
import com.ubs.tariffapp.exceptions.TariffNotFoundException;
import com.ubs.tariffapp.extensions.DockerRequiredExtension;
import com.ubs.tariffapp.models.dto.TariffRequest;
import com.ubs.tariffapp.models.dto.TariffResponse;
import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.services.TariffManagementService;

/**
 * Integration tests for TariffManagementController with GlobalExceptionHandler.
 * Tests validation, exception handling, and HTTP response formats.
 * 
 * NOTE: Even though services are mocked, this test still needs Docker because
 * the 'test' profile uses TestContainers to initialize the database connection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@ExtendWith(DockerRequiredExtension.class)
@DisplayName("TariffManagementController Integration Tests")
class TariffManagementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TariffManagementService tariffManagementService;

    @MockBean
    private DutyService dutyService;

    @MockBean
    private com.ubs.tariffapp.repositories.TariffScheduleRepository tariffScheduleRepository;

    private TariffRequest createValidTariffRequest() {
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

    private TariffResponse createMockTariffResponse() {
        return TariffResponse.builder()
                .tariffId(1)
                .tariffYear(2024)
                .reporterCode("USA")
                .reporterName("United States")
                .partnerCode("CHN")
                .partnerName("China")
                .tlCode("010121")
                .productDescription("Live horses")
                .dutyType("0")
                .dutyCode("0")
                .dutyTypeDescription("Standard (MFN)")
                .dutyCategory("AD_VALOREM")
                .adValoremRate(5.0)
                .build();
    }

    @Nested
    @DisplayName("POST /api/admin/tariffs - Create Tariff")
    class CreateTariffTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should create tariff successfully with valid request")
        void testCreateTariffSuccess() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            TariffResponse response = createMockTariffResponse();
            
            when(tariffManagementService.createTariff(any(TariffRequest.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tariffId").value(1))
                    .andExpect(jsonPath("$.tariffYear").value(2024))
                    .andExpect(jsonPath("$.reporterCode").value("USA"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 with validation errors when required fields are missing")
        void testCreateTariffMissingRequiredFields() throws Exception {
            // Arrange - Missing required fields
            TariffRequest request = new TariffRequest();
            request.setNote("Only note provided");

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").exists())
                    .andExpect(jsonPath("$.errors.tariffYear").exists())
                    .andExpect(jsonPath("$.errors.reporterCode").exists())
                    .andExpect(jsonPath("$.errors.partnerCode").exists())
                    .andExpect(jsonPath("$.errors.tlCode").exists())
                    .andExpect(jsonPath("$.errors.dutyType").exists())
                    .andExpect(jsonPath("$.errors.dutyCode").exists());
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when tariffYear is below minimum (2000)")
        void testCreateTariffInvalidYearTooLow() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setTariffYear(1999);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.tariffYear").value("Year must be 2000 or later"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when tariffYear exceeds maximum (2100)")
        void testCreateTariffInvalidYearTooHigh() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setTariffYear(2101);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.errors.tariffYear").value("Year must be 2100 or earlier"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when reporterCode is not 3 characters")
        void testCreateTariffInvalidReporterCodeLength() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setReporterCode("US"); // Only 2 characters

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.errors.reporterCode").value("Reporter code must be exactly 3 characters"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when partnerCode is not 3 characters")
        void testCreateTariffInvalidPartnerCodeLength() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setPartnerCode("CHIN"); // 4 characters

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.errors.partnerCode").value("Partner code must be exactly 3 characters"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when adValoremRate is negative")
        void testCreateTariffNegativeAdValoremRate() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(-5.0);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.errors.adValoremRate").value("Ad valorem rate must be non-negative"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when adValoremRate exceeds 100%")
        void testCreateTariffAdValoremRateExceeds100() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(150.0);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.errors.adValoremRate").value("Ad valorem rate cannot exceed 100%"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when specificRate is negative")
        void testCreateTariffNegativeSpecificRate() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(null);
            request.setSpecificRate(-10.0);
            request.setSpecificRateUnit("kg");

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.errors.specificRate").value("Specific rate must be non-negative"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when compoundRate1 is negative")
        void testCreateTariffNegativeCompoundRate1() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(null);
            request.setCompoundRate1(-5.0);
            request.setCompoundRate2(10.0);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.errors.compoundRate1").value("Compound rate 1 must be non-negative"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 with business logic error from service")
        void testCreateTariffBusinessLogicError() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            request.setAdValoremRate(null);
            request.setCompoundRate1(5.0);
            // Missing compoundRate2 - business logic violation
            
            when(tariffManagementService.createTariff(any(TariffRequest.class)))
                    .thenThrow(new InvalidRequestException("Both compound rates must be specified together"));

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Both compound rates must be specified together"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should accept valid tariff with boundary values")
        void testCreateTariffBoundaryValues() throws Exception {
            // Arrange - Test boundary values
            TariffRequest request = createValidTariffRequest();
            request.setTariffYear(2000); // Min year
            request.setAdValoremRate(100.0); // Max rate
            
            TariffResponse response = createMockTariffResponse();
            when(tariffManagementService.createTariff(any(TariffRequest.class)))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tariffId").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/tariffs/{id} - Update Tariff")
    class UpdateTariffTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should update tariff successfully")
        void testUpdateTariffSuccess() throws Exception {
            // Arrange
            TariffResponse response = createMockTariffResponse();
            when(tariffManagementService.updateTariff(anyInt(), anyMap()))
                    .thenReturn(response);

            String updateJson = """
                {
                    "note": "Updated note",
                    "tlsSuffix": "A"
                }
                """;

            // Act & Assert
            mockMvc.perform(put("/api/admin/tariffs/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffId").value(1));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 404 when tariff not found")
        void testUpdateTariffNotFound() throws Exception {
            // Arrange
            when(tariffManagementService.updateTariff(anyInt(), anyMap()))
                    .thenThrow(new TariffNotFoundException("Tariff not found with id: 999"));

            String updateJson = """
                {
                    "note": "Updated note"
                }
                """;

            // Act & Assert
            mockMvc.perform(put("/api/admin/tariffs/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Tariff not found with id: 999"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 400 when invalid rate provided in update")
        void testUpdateTariffInvalidRate() throws Exception {
            // Arrange
            when(tariffManagementService.updateTariff(anyInt(), anyMap()))
                    .thenThrow(new InvalidRequestException("Ad valorem rate must be between 0 and 100"));

            String updateJson = """
                {
                    "adValoremRate": 150.0
                }
                """;

            // Act & Assert
            mockMvc.perform(put("/api/admin/tariffs/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updateJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tariffs/{id} - Get Tariff by ID")
    class GetTariffTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should get tariff successfully")
        void testGetTariffSuccess() throws Exception {
            // Arrange
            TariffResponse response = createMockTariffResponse();
            when(tariffManagementService.getTariffById(1))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tariffId").value(1))
                    .andExpect(jsonPath("$.reporterCode").value("USA"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 404 when tariff not found")
        void testGetTariffNotFound() throws Exception {
            // Arrange
            when(tariffManagementService.getTariffById(999))
                    .thenThrow(new TariffNotFoundException("Tariff not found with id: 999"));

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/tariffs/{id} - Delete Tariff")
    class DeleteTariffTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should delete tariff successfully")
        void testDeleteTariffSuccess() throws Exception {
            // Arrange
            doNothing().when(tariffManagementService).deleteTariff(1);

            // Act & Assert
            mockMvc.perform(delete("/api/admin/tariffs/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return 404 when deleting non-existent tariff")
        void testDeleteTariffNotFound() throws Exception {
            // Arrange
            doThrow(new TariffNotFoundException("Tariff not found with id: 999"))
                    .when(tariffManagementService).deleteTariff(999);

            // Act & Assert
            mockMvc.perform(delete("/api/admin/tariffs/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tariffs - Get All Tariffs")
    class GetAllTariffsTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should get all tariffs successfully")
        void testGetAllTariffsSuccess() throws Exception {
            // Arrange
            TariffResponse response = createMockTariffResponse();
            when(tariffManagementService.getAllTariffs())
                    .thenReturn(Collections.singletonList(response));

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.tariffs").isArray())
                    .andExpect(jsonPath("$.count").value(1));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should handle internal server error gracefully")
        void testGetAllTariffsInternalError() throws Exception {
            // Arrange
            when(tariffManagementService.getAllTariffs())
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"));
        }
    }

    @Nested
    @DisplayName("Exception Handler Tests")
    class ExceptionHandlerTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should handle multiple validation errors and return all field errors")
        void testMultipleValidationErrors() throws Exception {
            // Arrange - Request with multiple invalid fields
            TariffRequest request = new TariffRequest();
            request.setTariffYear(1999); // Too low
            request.setReporterCode("US"); // Too short
            request.setAdValoremRate(-10.0); // Negative

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.tariffYear").exists())
                    .andExpect(jsonPath("$.errors.reporterCode").exists())
                    .andExpect(jsonPath("$.errors.adValoremRate").exists());
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should format InvalidRequestException with proper error response")
        void testInvalidRequestExceptionFormat() throws Exception {
            // Arrange
            TariffRequest request = createValidTariffRequest();
            when(tariffManagementService.createTariff(any(TariffRequest.class)))
                    .thenThrow(new InvalidRequestException("Reporter country not found: XYZ"));

            // Act & Assert
            mockMvc.perform(post("/api/admin/tariffs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Reporter country not found: XYZ"));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should format TariffNotFoundException with proper error response")
        void testTariffNotFoundExceptionFormat() throws Exception {
            // Arrange
            when(tariffManagementService.getTariffById(999))
                    .thenThrow(new TariffNotFoundException("Tariff not found with id: 999"));

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Tariff not found with id: 999"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/tariffs/years - Get Available Years")
    class GetAvailableYearsTests {

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return available years with success status")
        void testGetAvailableYears_Success() throws Exception {
            // Arrange
            java.util.List<Integer> mockYears = java.util.Arrays.asList(2024, 2023, 2022);
            when(tariffScheduleRepository.findDistinctYears()).thenReturn(mockYears);

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/years"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.years").isArray())
                    .andExpect(jsonPath("$.years.length()").value(3))
                    .andExpect(jsonPath("$.years[0]").value(2024))
                    .andExpect(jsonPath("$.years[1]").value(2023))
                    .andExpect(jsonPath("$.years[2]").value(2022))
                    .andExpect(jsonPath("$.count").value(3));
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should return empty array when no years available")
        void testGetAvailableYears_EmptyDatabase() throws Exception {
            // Arrange
            when(tariffScheduleRepository.findDistinctYears()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/years"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.years").isArray())
                    .andExpect(jsonPath("$.years.length()").value(0))
                    .andExpect(jsonPath("$.count").value(0));
        }

        @Test
        @WithMockUser(authorities = "Users")
        @DisplayName("Should return 403 Forbidden for non-admin users")
        void testGetAvailableYears_Forbidden() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/years"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Access denied"));
        }

        @Test
        @DisplayName("Should return 401 Unauthorized for unauthenticated requests")
        void testGetAvailableYears_Unauthorized() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/years"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(authorities = "Admins")
        @DisplayName("Should handle service exceptions gracefully")
        void testGetAvailableYears_ServiceException() throws Exception {
            // Arrange
            when(tariffScheduleRepository.findDistinctYears())
                    .thenThrow(new RuntimeException("Database connection error"));

            // Act & Assert
            mockMvc.perform(get("/api/admin/tariffs/years"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"));
        }
    }
}
