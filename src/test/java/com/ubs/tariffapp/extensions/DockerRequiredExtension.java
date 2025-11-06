package com.ubs.tariffapp.extensions;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * JUnit 5 extension that verifies Docker is available before running tests.
 * Provides a clear error message if Docker Desktop is not running.
 */
public class DockerRequiredExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        try {
            // Check if Docker is available
            DockerClientFactory.instance().client();
        } catch (Exception e) {
            String errorMessage = "\n" +
                "================================================================\n" +
                "|          DOCKER IS NOT RUNNING OR NOT AVAILABLE              |\n" +
                "================================================================\n" +
                "|                                                              |\n" +
                "|  These tests require Docker to run TestContainers.           |\n" +
                "|                                                              |\n" +
                "|  Please:                                                     |\n" +
                "|  1. Install Docker Desktop (if not installed)                |\n" +
                "|  2. Start Docker Desktop                                     |\n" +
                "|  3. Wait for Docker to be fully running                      |\n" +
                "|  4. Re-run the tests                                         |\n" +
                "|                                                              |\n" +
                "================================================================\n" +
                "\nOriginal error: " + e.getMessage() + "\n";
            
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
