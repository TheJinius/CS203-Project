package com.ubs.tariffapp.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

// This test now uses the lightweight executeWithScraper path (no Spring context).

/** Simple stub for PythonScraperService logic. */
class StubPythonScraperService extends com.ubs.tariffapp.services.PythonScraperService {
    StubPythonScraperService() { super(null, null); }
    @Override
    public boolean scrapeAndProcessCountryData(String countryCode) { return !"FAIL".equalsIgnoreCase(countryCode); }
}

/**
 * Integration-style tests for the PipelineExecutor main/execute flow.
 *
 * These start a Spring context but stub out the PythonScraperService to avoid
 * external side effects while still exercising argument parsing, context
 * initialization, loop control and exit codes.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("PipelineExecutor Lightweight Integration Tests")
class PipelineExecutorIntegrationTest {

    private final StubPythonScraperService stub = new StubPythonScraperService();

    @Test
    @DisplayName("executeWithScraper returns 0 for single successful country")
    void executeReturnsZeroOnSingleSuccess() throws Exception {
        int exitCode = PipelineExecutor.executeWithScraper(stub, new String[]{"USA"});
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    @DisplayName("executeWithScraper returns 1 when at least one country fails")
    void executeReturnsOneOnMixedSuccessFailure() throws Exception {
        int exitCode = PipelineExecutor.executeWithScraper(stub, new String[]{"USA,FAIL"});
        assertThat(exitCode).isEqualTo(1);
    }

    @Test
    @DisplayName("ALL keyword expands default set and succeeds with stub")
    void allKeywordExpandsDefaults() throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        java.io.PrintStream original = System.out;
        System.setOut(new java.io.PrintStream(out));
        int code;
        try {
            code = PipelineExecutor.executeWithScraper(stub, new String[]{"ALL"});
        } finally {
            System.setOut(original);
        }
        assertThat(code).isEqualTo(0);
        String stdout = out.toString();
        assertThat(stdout).contains("Using default country set");
        assertThat(stdout).contains("USA");
    }
}
