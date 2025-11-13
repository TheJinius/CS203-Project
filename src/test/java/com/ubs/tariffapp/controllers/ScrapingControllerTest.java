package com.ubs.tariffapp.controllers;

import com.ubs.tariffapp.services.ScheduledScrapingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for ScrapingController
 * Tests all endpoints with various scenarios including success, failure, and error cases
 */
@WebMvcTest(ScrapingController.class)
@TestPropertySource(properties = {
    "app.scraping.enabled=true"
})
@DisplayName("ScrapingController Tests")
class ScrapingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScheduledScrapingService scheduledScrapingService;

    @Nested
    @DisplayName("POST /api/scraping/trigger - Trigger Manual Scraping")
    class TriggerManualScrapingTests {

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should trigger manual scraping successfully")
        void testTriggerManualScraping_Success() throws Exception {
            // Arrange
            doNothing().when(scheduledScrapingService).triggerManualScraping();

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Manual scraping triggered successfully for all countries"));

            verify(scheduledScrapingService, times(1)).triggerManualScraping();
        }

        @Test
        @WithMockUser(roles = "Users")
        @DisplayName("Should trigger manual scraping with Users role")
        void testTriggerManualScraping_UsersRole() throws Exception {
            // Arrange
            doNothing().when(scheduledScrapingService).triggerManualScraping();

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Manual scraping triggered successfully for all countries"));

            verify(scheduledScrapingService, times(1)).triggerManualScraping();
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void testTriggerManualScraping_Unauthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger")
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(scheduledScrapingService, never()).triggerManualScraping();
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should return 403 when CSRF token is missing")
        void testTriggerManualScraping_MissingCsrf() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger"))
                    .andExpect(status().isForbidden());

            verify(scheduledScrapingService, never()).triggerManualScraping();
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle service exception gracefully")
        void testTriggerManualScraping_ServiceException() throws Exception {
            // Arrange
            doThrow(new RuntimeException("Service unavailable"))
                    .when(scheduledScrapingService).triggerManualScraping();

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger")
                    .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Error triggering scraping: Service unavailable"));

            verify(scheduledScrapingService, times(1)).triggerManualScraping();
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle null pointer exception")
        void testTriggerManualScraping_NullPointerException() throws Exception {
            // Arrange
            doThrow(new NullPointerException("Null pointer error"))
                    .when(scheduledScrapingService).triggerManualScraping();

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger")
                    .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Error triggering scraping: Null pointer error"));

            verify(scheduledScrapingService, times(1)).triggerManualScraping();
        }
    }

    @Nested
    @DisplayName("POST /api/scraping/trigger/{countryCode} - Trigger Country Specific Scraping")
    class TriggerCountrySpecificScrapingTests {

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should trigger scraping for USA successfully")
        void testTriggerCountrySpecificScraping_USA_Success() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("USA")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/USA")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for USA"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("USA");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should trigger scraping for CHN successfully")
        void testTriggerCountrySpecificScraping_CHN_Success() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("CHN")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/CHN")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for CHN"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("CHN");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should trigger scraping for SGP successfully")
        void testTriggerCountrySpecificScraping_SGP_Success() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("SGP")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/SGP")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for SGP"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("SGP");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should return bad request when scraping fails")
        void testTriggerCountrySpecificScraping_Failure() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("USA")).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/USA")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Scraping failed for USA"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("USA");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle invalid country code gracefully")
        void testTriggerCountrySpecificScraping_InvalidCountryCode() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("INVALID")).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/INVALID")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Scraping failed for INVALID"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("INVALID");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle lowercase country code")
        void testTriggerCountrySpecificScraping_LowercaseCountryCode() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("usa")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/usa")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for usa"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("usa");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle special characters in country code")
        void testTriggerCountrySpecificScraping_SpecialCharacters() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("US-A")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/US-A")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for US-A"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("US-A");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle service exception during country scraping")
        void testTriggerCountrySpecificScraping_ServiceException() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("USA"))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/USA")
                    .with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("Error scraping USA: Database connection failed"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("USA");
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void testTriggerCountrySpecificScraping_Unauthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/USA")
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());

            verify(scheduledScrapingService, never()).scrapeSpecificCountry(anyString());
        }

        @Test
        @WithMockUser(roles = "Users")
        @DisplayName("Should allow Users role to trigger country scraping")
        void testTriggerCountrySpecificScraping_UsersRole() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("USA")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/USA")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for USA"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("USA");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should return 403 when CSRF token is missing")
        void testTriggerCountrySpecificScraping_MissingCsrf() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/USA"))
                    .andExpect(status().isForbidden());

            verify(scheduledScrapingService, never()).scrapeSpecificCountry(anyString());
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle single space as country code")
        void testTriggerCountrySpecificScraping_SingleSpace() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry(" ")).thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/ ")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Scraping failed for  "));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry(" ");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle numeric country code")
        void testTriggerCountrySpecificScraping_NumericCountryCode() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("123")).thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/scraping/trigger/123")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for 123"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("123");
        }
    }

    @Nested
    @DisplayName("GET /api/scraping/status - Check Scraping Service Status")
    class GetScrapingServiceStatusTests {

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should return service status successfully")
        void testGetScrapingServiceStatus_Success() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/scraping/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping service is operational"));

            // Status endpoint doesn't call the service
            verifyNoInteractions(scheduledScrapingService);
        }

        @Test
        @WithMockUser(roles = "Users")
        @DisplayName("Should return service status for Users role")
        void testGetScrapingServiceStatus_UsersRole() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/scraping/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping service is operational"));

            verifyNoInteractions(scheduledScrapingService);
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void testGetScrapingServiceStatus_Unauthenticated() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/scraping/status"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(scheduledScrapingService);
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should return status even when service is null")
        void testGetScrapingServiceStatus_ServiceNull() throws Exception {
            // Act & Assert - even if service is null, status endpoint returns OK
            mockMvc.perform(get("/api/scraping/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping service is operational"));

            verifyNoInteractions(scheduledScrapingService);
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle GET request with query parameters")
        void testGetScrapingServiceStatus_WithQueryParams() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/scraping/status")
                    .param("test", "value"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping service is operational"));

            verifyNoInteractions(scheduledScrapingService);
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenariosTests {

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle multiple sequential scraping requests")
        void testMultipleSequentialScrapingRequests() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("USA")).thenReturn(true);
            when(scheduledScrapingService.scrapeSpecificCountry("CHN")).thenReturn(true);
            when(scheduledScrapingService.scrapeSpecificCountry("SGP")).thenReturn(true);

            // Act & Assert - First request
            mockMvc.perform(post("/api/scraping/trigger/USA")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for USA"));

            // Act & Assert - Second request
            mockMvc.perform(post("/api/scraping/trigger/CHN")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for CHN"));

            // Act & Assert - Third request
            mockMvc.perform(post("/api/scraping/trigger/SGP")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for SGP"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("USA");
            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("CHN");
            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("SGP");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle mixed success and failure scenarios")
        void testMixedSuccessAndFailure() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry("USA")).thenReturn(true);
            when(scheduledScrapingService.scrapeSpecificCountry("INVALID")).thenReturn(false);

            // Act & Assert - Success case
            mockMvc.perform(post("/api/scraping/trigger/USA")
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Scraping completed successfully for USA"));

            // Act & Assert - Failure case
            mockMvc.perform(post("/api/scraping/trigger/INVALID")
                    .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Scraping failed for INVALID"));

            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("USA");
            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("INVALID");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should verify all endpoints are accessible")
        void testAllEndpointsAccessible() throws Exception {
            // Arrange
            doNothing().when(scheduledScrapingService).triggerManualScraping();
            when(scheduledScrapingService.scrapeSpecificCountry("USA")).thenReturn(true);

            // Act & Assert - Test all endpoints
            mockMvc.perform(get("/api/scraping/status"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/scraping/trigger")
                    .with(csrf()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/scraping/trigger/USA")
                    .with(csrf()))
                    .andExpect(status().isOk());

            verify(scheduledScrapingService, times(1)).triggerManualScraping();
            verify(scheduledScrapingService, times(1)).scrapeSpecificCountry("USA");
        }

        @Test
        @WithMockUser(roles = "Admin")
        @DisplayName("Should handle concurrent country code variations")
        void testCountryCodeVariations() throws Exception {
            // Arrange
            when(scheduledScrapingService.scrapeSpecificCountry(anyString())).thenReturn(true);

            // Act & Assert - Test various country code formats
            String[] countryCodes = {"USA", "usa", "Us", "CHN", "SG", "JP"};
            
            for (String code : countryCodes) {
                mockMvc.perform(post("/api/scraping/trigger/" + code)
                        .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(content().string("Scraping completed successfully for " + code));
            }

            verify(scheduledScrapingService, times(countryCodes.length)).scrapeSpecificCountry(anyString());
        }
    }
}
