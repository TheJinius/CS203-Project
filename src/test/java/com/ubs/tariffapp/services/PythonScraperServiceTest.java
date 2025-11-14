package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;

/**
 * Integration tests for PythonScraperService using stub Python scripts.
 * 
 * These tests use a stub Python script that simulates the real web scraper
 * without actually connecting to WITS or downloading real data.
 * Uses Testcontainers with PostgreSQL like other integration tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.python.script.path=src/test/resources/stub_scraper.py",
    "app.python.executable=python",
    "logging.level.com.ubs.tariffapp.services.PythonScraperService=DEBUG"
})
@ExtendWith(DockerRequiredExtension.class)
class PythonScraperServiceTest {
    
    @Autowired
    private PythonScraperService pythonScraperService;
    
    @MockBean
    private DataLoaderService dataLoaderService;
    
    @BeforeEach
    void setUp() {
        // Mock the data loader to avoid actual database operations
        doNothing().when(dataLoaderService).loadCleanedData(anyString());
    }
    
    @AfterEach
    void cleanup() {
        // Clean up any test files created
        try {
            // Clean up test_data directory
            Path testDataDir = Paths.get("src/main/resources/data/test_data");
            if (Files.exists(testDataDir)) {
                Files.walk(testDataDir)
                    .filter(p -> p.getFileName().toString().startsWith("test_"))
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            }
            
            // Clean up clean_data directory
            Path cleanDataDir = Paths.get("src/main/resources/data/clean_data");
            if (Files.exists(cleanDataDir)) {
                Files.walk(cleanDataDir)
                    .filter(p -> p.getFileName().toString().startsWith("clean_test_"))
                    .filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    void testScrapeAndProcessCountryData_Success() {
        // Given a valid country code
        String countryCode = "USA";
        
        // When scraping and processing
        boolean result = pythonScraperService.scrapeAndProcessCountryData(countryCode);
        
        // Then the operation should succeed
        assertThat(result).isTrue();
        
        // And the data loader should be called with the cleaned filename
        verify(dataLoaderService).loadCleanedData(contains("clean_test_usa_data.csv"));
    }
    
    @Test
    void testScrapeAndProcessCountryData_DifferentCountries() {
        // Test with different country codes
        String[] countries = {"CHN", "SGP", "JPN"};
        
        for (String country : countries) {
            boolean result = pythonScraperService.scrapeAndProcessCountryData(country);
            assertThat(result).isTrue();
        }
        
        // Verify data loader was called for each country
        verify(dataLoaderService, times(3)).loadCleanedData(anyString());
    }
}
