package com.ubs.tariffapp.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HSDataCleaner main() Integration Test")
public class HSDataCleanerIntegrationTest {

    private Path createdInputFile;
    private Path createdOutputFile;

    @AfterEach
    void cleanup() throws IOException {
        if (createdOutputFile != null && Files.exists(createdOutputFile)) {
            Files.delete(createdOutputFile);
        }
        // Input is deleted by main(); ensure directory remains
    }

    @Test
    @DisplayName("main() should process input CSV and generate cleaned output with new columns")
    void mainShouldProcessCsvAndGenerateOutput() throws Exception {
        // Arrange: create test_data directory and a minimal valid CSV
        String testFileName = "HSDataCleanerTestInput.csv";
        Path testDataDir = Paths.get("src/main/resources/data/test_data");
        Files.createDirectories(testDataDir);

        createdInputFile = testDataDir.resolve(testFileName);

        // Build header with 16 columns (0..15 as described in HSDataCleaner javadoc)
        List<String> header = Arrays.asList(
            "Reporter",
            "ReporterName",
            "Partner",
            "Partner Name",
            "Year",
            "TL (HS Code)",
            "TLS (Additional HS Code Sub-classification - if any)",
            "Duty Type",
            "Duty Code",
            "Ad Valorem Duty Rate (%)",
            "Specific Duty Rate",
            "HS Code Description",
            "Duty Type Description",
            "Duty Nature",
            "Ad Valorem Calculation Code/Description",
            "Notes"
        );

        // One data row: Reporter=840 (USA), Partner=156 (China), Year=2023, HS Code=72000000 (Metals), AV Rate=5%
        String[] row = new String[16];
        row[0] = "840";           // Reporter (WITS) -> USA
        row[1] = "United States"; // ReporterName (will be overwritten)
        row[2] = "156";           // Partner (WITS) -> China
        row[3] = "China";         // Partner Name (will be overwritten)
        row[4] = "2023";          // Year
        row[5] = "72000000";      // HS code (Metals)
        row[6] = "0";             // TLS
        row[7] = "0";             // Duty Type
        row[8] = "2";             // Duty Code
        row[9] = "5";             // Ad Valorem Duty Rate (%) as numeric string
        row[10] = "";             // Specific Duty Rate
        row[11] = "Hot-rolled steel"; // HS Code Description
        row[12] = "";             // Duty Type Description
        row[13] = "";             // Duty Nature
        row[14] = "";             // AV Calc Code/Desc
        row[15] = "";             // Notes

        String headerLine = String.join(",", header);
        String dataLine = String.join(",", row);
        Files.write(createdInputFile, Arrays.asList(headerLine, dataLine), StandardCharsets.UTF_8);

        // Act: run main with the test file name
        HSDataCleaner.main(new String[] { testFileName });

        // Assert: input should be deleted by main
        assertThat(Files.exists(createdInputFile)).isFalse();

        // Assert: output file exists with appended columns
        createdOutputFile = Paths.get("src/main/resources/data/clean_data/clean_" + testFileName);
        assertThat(Files.exists(createdOutputFile)).isTrue();

        List<String> lines = Files.readAllLines(createdOutputFile, StandardCharsets.UTF_8);
        assertThat(lines.size()).isGreaterThanOrEqualTo(2);

        String outHeader = lines.get(0);
        String outData = lines.get(1);
        String[] outHeaderCols = outHeader.split(",", -1);
        String[] outCols = outData.split(",", -1);

        // Original 16 columns + 9 appended = 25 columns total
        assertThat(outHeaderCols.length).isEqualTo(25);
        assertThat(outCols.length).isEqualTo(25);

        int base = 16; // index where new columns start
        // Reporter ISO and Partner ISO
        assertThat(outCols[base]).isEqualTo("USA"); // Reporter ISO for 840
        assertThat(outCols[base + 1]).isEqualTo("CHN"); // Partner ISO for 156

        // Industry should be Metals for HS chapter 72
        assertThat(outCols[base + 2]).isEqualTo("Metals");

        // Duty type and standardized AV rate for 5%
        assertThat(outCols[base + 3]).isEqualTo("AD_VALOREM");
        assertThat(outCols[base + 4]).isEqualTo("5"); // formatted number

        // Specific duty amount should be 0 for AV-only case
        assertThat(outCols[base + 5]).isIn("0", "0.0");

        // Currency and Unit can be empty for AV-only
        assertThat(outCols[base + 6]).isIn("", null, "USD"); // tolerant if parser sets a currency
        assertThat(outCols[base + 7]).isIn("", null);

        // Original specific duty field should mirror the original column[10]
        assertThat(outCols[base + 8]).isEqualTo("");
    }
}
