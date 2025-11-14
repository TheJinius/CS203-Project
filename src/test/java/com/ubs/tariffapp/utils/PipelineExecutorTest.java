package com.ubs.tariffapp.utils;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for PipelineExecutor.execute() method.
 * 
 * These tests verify the argument validation logic.
 * Integration tests with real Spring context and services are handled separately
 * due to the complexity of mocking the Spring Boot application context.
 */
@DisplayName("PipelineExecutor Tests")
public class PipelineExecutorTest {

    @Test
    @DisplayName("Returns 1 when no arguments provided")
    void returnsErrorOnNoArgs() {
        int exitCode = PipelineExecutor.execute(new String[]{});
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    @DisplayName("Returns 1 when empty string argument provided")
    void returnsErrorOnEmptyArg() {
        int exitCode = PipelineExecutor.execute(new String[]{""});
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    @DisplayName("Returns 1 when whitespace-only argument provided")
    void returnsErrorOnWhitespaceArg() {
        int exitCode = PipelineExecutor.execute(new String[]{"   "});
        assertThat(exitCode).isEqualTo(1);
    }
}
