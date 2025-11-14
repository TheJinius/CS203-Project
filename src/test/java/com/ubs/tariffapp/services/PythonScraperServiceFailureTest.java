package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;

/**
 * Tests for PythonScraperService failure scenarios.
 * Uses a stub script that simulates failures.
 * Uses Testcontainers with PostgreSQL like other integration tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.python.script.path=src/test/resources/stub_scraper_fail.py",
    "app.python.executable=python",
    "logging.level.com.ubs.tariffapp.services.PythonScraperService=DEBUG"
})
@ExtendWith(DockerRequiredExtension.class)
class PythonScraperServiceFailureTest {
    
    @Autowired
    private PythonScraperService pythonScraperService;
    
    @MockBean
    private DataLoaderService dataLoaderService;
    
    @BeforeEach
    void setUp() {
        doNothing().when(dataLoaderService).loadCleanedData(anyString());
    }
    
    @Test
    void testScrapeAndProcessCountryData_ScriptFailure() {
        // Given the stub script will fail
        String countryCode = "USA";
        
        // When scraping and processing
        boolean result = pythonScraperService.scrapeAndProcessCountryData(countryCode);
        
        // Then the operation should fail after retries
        assertThat(result).isFalse();
        
        // And the data loader should never be called
        verify(dataLoaderService, never()).loadCleanedData(anyString());
    }
    
    @Test
    void testScrapeAndProcessCountryData_MultipleFailures() {
        // Test that multiple countries all fail consistently
        String[] countries = {"CHN", "SGP", "JPN"};
        
        for (String country : countries) {
            boolean result = pythonScraperService.scrapeAndProcessCountryData(country);
            assertThat(result).isFalse();
        }
        
        // Verify data loader was never called
        verify(dataLoaderService, never()).loadCleanedData(anyString());
    }
}
