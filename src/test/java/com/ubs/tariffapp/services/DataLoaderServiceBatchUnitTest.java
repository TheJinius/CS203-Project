package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;
import com.ubs.tariffapp.models.duty.OtherDuty;
import com.ubs.tariffapp.repositories.duty.OtherDutyRepository;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(DockerRequiredExtension.class)
class DataLoaderServiceBatchUnitTest {

    @Autowired
    private DataLoaderService dataLoaderService;

    @Autowired
    private OtherDutyRepository otherDutyRepository;

    private String buildLine(
            String reporter, String reporterName,
            String partner, String partnerName,
            String year, String tlCode, String tlsSuffix,
            String dutyType, String dutyCode,
            String avDutyRate, String specificDutyRate,
            String description, String dutyTypeDesc,
            String dutyNature, String avMethod,
            String note,
            String reporterISO, String partnerISO,
            String industry, String cleanedDutyType,
            String standardizedAVRate, String specificDutyAmount,
            String currency, String unit,
            String originalSpecificDuty) {

        List<String> cols = new ArrayList<>(List.of(reporter, reporterName,
                partner, partnerName,
                year, tlCode, tlsSuffix,
                dutyType, dutyCode,
                avDutyRate, specificDutyRate,
                description, dutyTypeDesc,
                dutyNature, avMethod,
                note,
                reporterISO, partnerISO,
                industry, cleanedDutyType,
                standardizedAVRate, specificDutyAmount,
                currency, unit,
                originalSpecificDuty));
        return cols.stream().collect(Collectors.joining(","));
    }

    private String quote(String s) {
        return "\"" + s + "\"";
    }

    @BeforeEach
    void quiet() {
        // No-op; placeholder for future per-test setup
    }

    @Test
    @DisplayName("processBatchSafely falls back to individual on duplicate flush")
    void processBatchSafely_duplicateTriggersFallback() {
        // Two identical lines to cause a duplicate key on flush
        String line = buildLine(
            "901", quote("Rep A"),
            "902", quote("Partner B"),
            "2024", "01012100", "01",
            "0", "0",
            "5.0", quote("3 USD/kg"),
            quote("Test product"), quote("Standard"),
            "A", "C", quote("Note"),
            "RA", "PB",
            "Agriculture", "AD_VALOREM",
            "7.5", "", "USD", "kg",
            quote("3 USD/kg"));

        List<String> batch = List.of(line, line);
        int[] results = dataLoaderService.processBatchSafely(batch, 0);
        assertThat(results[0] + results[1]).isEqualTo(2);
        // One inserted, one skipped due to duplicate
        assertThat(results[0]).isEqualTo(1);
        assertThat(results[1]).isEqualTo(1);
    }

    @Test
    @DisplayName("Combined duty handles Mixed and Compound")
    void combinedDuty_mixedAndCompound() {
        long beforeCount = dataLoaderService.getCombinedDutyCount();

        String baseReporter = "911"; // unique per test
        String lineMixed = buildLine(
            baseReporter, quote("Rep M"),
            "912", quote("Partner M"),
            "2024", "02020202", "0",
            "0", "0",
            "10.0", quote("2 USD/kg"),
            quote("Prod Mixed"), quote("Standard"),
            "C", "M", quote("Note"),
            "RM", "PM",
            "Agriculture", "MIXED",
            "", "2.0", "USD", "kg",
            quote("2 USD/kg"));

        String lineCompound = buildLine(
            baseReporter, quote("Rep C"),
            "913", quote("Partner C"),
            "2024", "03030303", "0",
            "0", "0",
            "8.0", quote("1 USD/kg"),
            quote("Prod Compound"), quote("Standard"),
            "C", "C", quote("Note"),
            "RM", "PC",
            "Agriculture", "MIXED",
            "7.0", "1.0", "USD", "kg",
            quote("1 USD/kg"));

        int[] results = dataLoaderService.processBatchSafely(List.of(lineMixed, lineCompound), 0);
        assertThat(results[0]).isEqualTo(2);
        assertThat(dataLoaderService.getCombinedDutyCount()).isEqualTo(beforeCount + 2);
    }

    @Test
    @DisplayName("Other duty computable vs non-computable classification")
    void otherDuty_computableClassification() {
        long before = dataLoaderService.getOtherDutyCount();

        String marker1 = "XYZTEST-COMP";
        String marker2 = "XYZTEST-NONCOMP";

        String computable = buildLine(
            "921", quote("Rep O1"),
            "922", quote("Partner O1"),
            "2024", "04040404", "",
            "0", "2",
            "", "5 per kg " + marker1,
            quote("Prod O1"), quote("Duty Free"),
            "O", "", quote("Note"),
            "RO1", "PO1",
            "Agriculture", "CONDITIONAL",
            "", "", "USD", "kg",
            quote("5 per kg " + marker1));

        String nonComputable = buildLine(
            "923", quote("Rep O2"),
            "924", quote("Partner O2"),
            "2024", "05050505", "",
            "3", "0",
            "", "See heading 999 " + marker2,
            quote("Prod O2"), quote("Temporary"),
            "O", "", quote("Note"),
            "RO2", "PO2",
            "Agriculture", "CONDITIONAL",
            "", "", "USD", "kg",
            quote("See heading 999 " + marker2));

        int[] results = dataLoaderService.processBatchSafely(List.of(computable, nonComputable), 0);
        assertThat(results[0]).isEqualTo(2);
        long after = dataLoaderService.getOtherDutyCount();
        assertThat(after).isEqualTo(before + 2);

        List<OtherDuty> all = otherDutyRepository.findAll();
        boolean foundComp = all.stream().anyMatch(d -> d.getRawText() != null && d.getRawText().contains(marker1) && Boolean.TRUE.equals(d.getIsComputable()));
        boolean foundNonComp = all.stream().anyMatch(d -> d.getRawText() != null && d.getRawText().contains(marker2) && Boolean.FALSE.equals(d.getIsComputable()));
        assertThat(foundComp).isTrue();
        assertThat(foundNonComp).isTrue();
    }
}
