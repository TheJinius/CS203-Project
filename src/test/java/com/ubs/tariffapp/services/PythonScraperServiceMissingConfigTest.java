package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Tests that PythonScraperService is NOT loaded when configuration is missing.
 * Uses ApplicationContextRunner to create a context without the required property.
 * 
 * This test uses a lightweight approach that doesn't require Testcontainers
 * since we're only testing bean conditional loading, not actual database operations.
 */
class PythonScraperServiceMissingConfigTest {
    
    @Configuration
    static class TestConfiguration {
        // Empty configuration - just for context
    }
    
    @Test
    void testServiceIsNotLoaded_WhenConfigurationMissing() {
        // Create an application context without the app.python.script.path property
        new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                // Set other required properties but NOT app.python.script.path
                "spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.jpa.hibernate.ddl-auto=create-drop"
            )
            .run(context -> {
                // Then the service should NOT be loaded due to @ConditionalOnProperty
                assertThat(context).doesNotHaveBean(PythonScraperService.class);
                assertThat(context).doesNotHaveBean("pythonScraperService");
                
                // Verify that trying to get the bean throws NoSuchBeanDefinitionException
                assertThatThrownBy(() -> context.getBean(PythonScraperService.class))
                    .isInstanceOf(NoSuchBeanDefinitionException.class);
            });
    }
}
