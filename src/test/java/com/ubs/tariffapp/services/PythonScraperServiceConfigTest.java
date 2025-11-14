package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;

/**
 * Tests for PythonScraperService configuration and conditional loading.
 * Uses Testcontainers with PostgreSQL like other integration tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.python.script.path=src/test/resources/stub_scraper.py",
    "app.python.executable=python"
})
@ExtendWith(DockerRequiredExtension.class)
class PythonScraperServiceConfigTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void testServiceIsLoaded_WhenConfigurationPresent() {
        // When configuration properties are present
        // Then the service should be loaded
        assertThat(applicationContext.containsBean("pythonScraperService")).isTrue();
        
        PythonScraperService service = applicationContext.getBean(PythonScraperService.class);
        assertThat(service).isNotNull();
    }
}
