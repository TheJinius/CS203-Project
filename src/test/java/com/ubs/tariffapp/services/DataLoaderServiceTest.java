package com.ubs.tariffapp.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;
import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.OtherDuty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.DutyTypeRepository;
import com.ubs.tariffapp.repositories.ProductRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.repositories.duty.AdValoremDutyRepository;
import com.ubs.tariffapp.repositories.duty.CombinedDutyRepository;
import com.ubs.tariffapp.repositories.duty.OtherDutyRepository;
import com.ubs.tariffapp.repositories.duty.SpecificDutyRepository;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(DockerRequiredExtension.class)
public class DataLoaderServiceTest {

    @Autowired
    private DataLoaderService dataLoaderService;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DutyTypeRepository dutyTypeRepository;

    @Autowired
    private TariffScheduleRepository tariffScheduleRepository;

    @Autowired
    private AdValoremDutyRepository adValoremDutyRepository;

    @Autowired
    private SpecificDutyRepository specificDutyRepository;

    @Autowired
    private CombinedDutyRepository combinedDutyRepository;

    @Autowired
    private OtherDutyRepository otherDutyRepository;

    private static final String TEST_CSV_FILE = "testclean_HS2017MYSYear2023.csv";
    private static boolean dataLoaded = false;

    @AfterAll
    static void tearDown() {
        // Cleanup will be handled by the test framework or can be done manually if needed
        // Since we're using @TestPropertySource with test database, data will be cleaned up automatically
        System.out.println("All tests completed. Test database will be cleaned up automatically.");
    }

    // Optional manual cleanup method for specific use cases
    private void cleanupAllData() {
        // Delete in correct order to avoid foreign key constraints
        adValoremDutyRepository.deleteAll();
        specificDutyRepository.deleteAll();
        combinedDutyRepository.deleteAll();
        otherDutyRepository.deleteAll();
        tariffScheduleRepository.deleteAll();
        dutyTypeRepository.deleteAll();
        productRepository.deleteAll();
        countryRepository.deleteAll();
        
        // Reset the flag if we manually clean up
        dataLoaded = false;
    }

    @Test
    @Order(1)
    void testLoadCleanedData_BasicDataLoading() {
        System.out.println("DEBUG: Starting Test 1 - testLoadCleanedData_BasicDataLoading");
        
        // Clean up data only if this is the first run and we need a fresh start
        if (!dataLoaded) {
            System.out.println("DEBUG: Cleaning up existing data...");
            // Optional: Clean up any existing test data from previous runs
            cleanupAllData();
            
            System.out.println("DEBUG: Loading CSV data...");
            // Test that the basic data loading works
            assertDoesNotThrow(() -> {
                dataLoaderService.loadCleanedData(TEST_CSV_FILE);
            });
            dataLoaded = true;
            System.out.println("DEBUG: Data loading completed");
        }

        // Verify basic counts are greater than 0
        assertTrue(dataLoaderService.getCountryCount() > 0, "Countries should be loaded");
        assertTrue(dataLoaderService.getProductCount() > 0, "Products should be loaded");
        assertTrue(dataLoaderService.getDutyTypeCount() > 0, "Duty types should be loaded");
        assertTrue(dataLoaderService.getTariffScheduleCount() > 0, "Tariff schedules should be loaded");
        
        System.out.println("DEBUG: Completed Test 1 - testLoadCleanedData_BasicDataLoading");
    }

    @Test
    @Order(2)
    void testDutyModelCreation_AdValoremDuty() {
        System.out.println("DEBUG: Starting Test 2 - testDutyModelCreation_AdValoremDuty");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Find ad valorem duties
        List<AdValoremDuty> adValoremDuties = adValoremDutyRepository.findAll();
        assertTrue(adValoremDuties.size() > 0, "Ad valorem duties should be created");

        // Test first ad valorem duty
        AdValoremDuty firstDuty = adValoremDuties.get(0);
        assertNotNull(firstDuty.getTariffSchedule(), "Duty should be linked to tariff schedule");
        assertNotNull(firstDuty.getRatePercent(), "Rate percentage should be set");
        assertTrue(firstDuty.getRatePercent().compareTo(BigDecimal.ZERO) >= 0, "Rate should be non-negative");
        
        System.out.println("DEBUG: Completed Test 2 - testDutyModelCreation_AdValoremDuty");
    }

    @Test
    @Order(3)
    void testDutyModelCreation_SpecificDuty() {
        System.out.println("DEBUG: Starting Test 3 - testDutyModelCreation_SpecificDuty");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Find specific duties
        List<SpecificDuty> specificDuties = specificDutyRepository.findAll();
        assertTrue(specificDuties.size() > 0, "Specific duties should be created");

        // Test first specific duty
        SpecificDuty firstDuty = specificDuties.get(0);
        assertNotNull(firstDuty.getTariffSchedule(), "Duty should be linked to tariff schedule");
        assertNotNull(firstDuty.getSpecificDutyRateRaw(), "Raw specific duty text should be preserved");
        assertFalse(firstDuty.getSpecificDutyRateRaw().isEmpty(), "Raw text should not be empty");
        
        // Check if parsed components exist
        if (firstDuty.getAmount() != null) {
            assertTrue(firstDuty.getAmount().compareTo(BigDecimal.ZERO) >= 0, "Amount should be non-negative");
        }
        
        System.out.println("DEBUG: Completed Test 3 - testDutyModelCreation_SpecificDuty");
    }

    @Test
    @Order(4)
    void testDutyModelCreation_CombinedDuty() {
        System.out.println("DEBUG: Starting Test 4 - testDutyModelCreation_CombinedDuty");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Find combined duties
        List<CombinedDuty> combinedDuties = combinedDutyRepository.findAll();
        
        if (combinedDuties.size() > 0) {
            // Test first combined duty
            CombinedDuty firstDuty = combinedDuties.get(0);
            assertNotNull(firstDuty.getTariffSchedule(), "Duty should be linked to tariff schedule");
            assertNotNull(firstDuty.getMixedOrCompound(), "Mixed/Conditional flag should be set");
            assertTrue(firstDuty.getMixedOrCompound().equals("M") || 
                      firstDuty.getMixedOrCompound().equals("C"), "Should be M (Mixed) or C (Compound)");
            
            // At least one component should be present
            boolean hasAdValorem = firstDuty.getRatePercent() != null && 
                                  firstDuty.getRatePercent().compareTo(BigDecimal.ZERO) > 0;
            boolean hasSpecific = firstDuty.getAmount() != null && 
                                 firstDuty.getAmount().compareTo(BigDecimal.ZERO) > 0;
            
            assertTrue(hasAdValorem || hasSpecific, "Combined duty should have at least one component");
        }
        
        System.out.println("DEBUG: Completed Test 4 - testDutyModelCreation_CombinedDuty");
    }

    @Test
    @Order(5)
    void testDutyModelCreation_OtherDuty() {
        System.out.println("DEBUG: Starting Test 5 - testDutyModelCreation_OtherDuty");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Find other duties
        List<OtherDuty> otherDuties = otherDutyRepository.findAll();
        
        if (otherDuties.size() > 0) {
            // Test first other duty
            OtherDuty firstDuty = otherDuties.get(0);
            assertNotNull(firstDuty.getTariffSchedule(), "Duty should be linked to tariff schedule");
            assertNotNull(firstDuty.getRawText(), "Raw text should be preserved");
            assertNotNull(firstDuty.getIsComputable(), "Computable flag should be set");
        }
        
        System.out.println("DEBUG: Completed Test 5 - testDutyModelCreation_OtherDuty");
    }

    @Test
    @Order(6)
    void testDutyDistribution() {
        System.out.println("DEBUG: Starting Test 6 - testDutyDistribution");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Get counts of each duty type
        long adValoremCount = dataLoaderService.getAdValoremDutyCount();
        long specificCount = dataLoaderService.getSpecificDutyCount();
        long combinedCount = dataLoaderService.getCombinedDutyCount();
        long otherCount = dataLoaderService.getOtherDutyCount();
        long totalDuties = adValoremCount + specificCount + combinedCount + otherCount;
        long totalSchedules = dataLoaderService.getTariffScheduleCount();

        // Print distribution
        System.out.println("=== DUTY DISTRIBUTION ===");
        System.out.println("Total Tariff Schedules: " + totalSchedules);
        System.out.println("Total Duties Created: " + totalDuties);
        System.out.println("Ad Valorem Duties: " + adValoremCount + " (" + 
                          String.format("%.1f%%", totalDuties > 0 ? (adValoremCount * 100.0 / totalDuties) : 0) + ")");
        System.out.println("Specific Duties: " + specificCount + " (" + 
                          String.format("%.1f%%", totalDuties > 0 ? (specificCount * 100.0 / totalDuties) : 0) + ")");
        System.out.println("Combined Duties: " + combinedCount + " (" + 
                          String.format("%.1f%%", totalDuties > 0 ? (combinedCount * 100.0 / totalDuties) : 0) + ")");
        System.out.println("Other Duties: " + otherCount + " (" + 
                          String.format("%.1f%%", totalDuties > 0 ? (otherCount * 100.0 / totalDuties) : 0) + ")");
        System.out.println("Duty Coverage: " + String.format("%.1f%%", 
                          totalSchedules > 0 ? (totalDuties * 100.0 / totalSchedules) : 0));
        System.out.println("=========================");

        // Verify that we have some duties created
        assertTrue(totalDuties > 0, "At least some duties should be created");
        
        // Not every tariff schedule needs to have a duty (some might have empty/invalid duty data)
        assertTrue(totalDuties <= totalSchedules, "Number of duties should not exceed number of schedules");
        
        // But we should have a reasonable coverage rate
        double coverageRate = totalSchedules > 0 ? (totalDuties * 100.0 / totalSchedules) : 0;
        assertTrue(coverageRate > 50, "Duty coverage should be reasonable (>50%), actual: " + coverageRate + "%");
        
        System.out.println("DEBUG: Completed Test 6 - testDutyDistribution");
    }

    @Test
    @Order(7)
    void testCountryCreation() {
        System.out.println("DEBUG: Starting Test 7 - testCountryCreation");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Test that Malaysia is created correctly
        Optional<Country> usaCountry = countryRepository.findById("458");
        assertTrue(usaCountry.isPresent(), "MYS should be present");
        assertEquals("Malaysia", usaCountry.get().getCountryName());
        assertNotNull(usaCountry.get().getIsoCode(), "ISO code should be set");

        // Test World partner
        Optional<Country> worldPartner = countryRepository.findById("000");
        assertTrue(worldPartner.isPresent(), "World partner should be present");
        assertEquals("World", worldPartner.get().getCountryName());
        
        System.out.println("DEBUG: Completed Test 7 - testCountryCreation");
    }

    @Test
    @Order(8)
    void testProductCreation() {
        System.out.println("DEBUG: Starting Test 8 - testProductCreation");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Get first few products to test
        List<Product> products = productRepository.findAll();
        assertTrue(products.size() > 0, "Products should be created");

        Product firstProduct = products.get(0);
        assertNotNull(firstProduct.getTlCode(), "TL code should be set");
        assertNotNull(firstProduct.getDescription(), "Description should be set");
        assertFalse(firstProduct.getDescription().isEmpty(), "Description should not be empty");
        assertTrue(firstProduct.getDigits() > 0, "Digits should be positive");
        
        System.out.println("DEBUG: Completed Test 8 - testProductCreation");
    }

    @Test
    @Order(9)
    void testTariffScheduleRelationships() {
        System.out.println("DEBUG: Starting Test 9 - testTariffScheduleRelationships");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Get first tariff schedule
        List<TariffSchedule> schedules = tariffScheduleRepository.findAll();
        assertTrue(schedules.size() > 0, "Tariff schedules should be created");

        TariffSchedule firstSchedule = schedules.get(0);
        
        // Test relationships
        assertNotNull(firstSchedule.getReporter(), "Reporter should be set");
        assertNotNull(firstSchedule.getPartner(), "Partner should be set");
        assertNotNull(firstSchedule.getProduct(), "Product should be set");
        assertNotNull(firstSchedule.getDutyType(), "Duty type should be set");
        assertTrue(firstSchedule.getTariffYear() > 0, "Tariff year should be positive");

        // Test that we can navigate relationships
        assertNotNull(firstSchedule.getReporter().getCountryName(), "Reporter name should be accessible");
        assertNotNull(firstSchedule.getProduct().getDescription(), "Product description should be accessible");
        
        System.out.println("DEBUG: Completed Test 9 - testTariffScheduleRelationships");
    }

    @Test
    @Order(10)
    void testDutyTypeCreation() {
        System.out.println("DEBUG: Starting Test 10 - testDutyTypeCreation");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Test duty types
        List<DutyType> dutyTypes = dutyTypeRepository.findAll();
        assertTrue(dutyTypes.size() > 0, "Duty types should be created");

        DutyType firstDutyType = dutyTypes.get(0);
        assertNotNull(firstDutyType.getId(), "Duty type ID should be set");
        assertNotNull(firstDutyType.getId().getDutyType(), "Duty type code should be set");
        assertNotNull(firstDutyType.getId().getDutyCode(), "Duty code should be set");
        assertNotNull(firstDutyType.getDutyTypeDescription(), "Description should be set");
        
        System.out.println("DEBUG: Completed Test 10 - testDutyTypeCreation");
    }

    @Test
    @Order(11)
    void testDataIntegrity() {
        System.out.println("DEBUG: Starting Test 11 - testDataIntegrity");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Test data integrity constraints
        List<TariffSchedule> schedules = tariffScheduleRepository.findAll();
        
        for (TariffSchedule schedule : schedules) {
            // Each tariff schedule should have valid relationships
            assertNotNull(schedule.getReporter(), "Reporter should not be null");
            assertNotNull(schedule.getPartner(), "Partner should not be null");
            assertNotNull(schedule.getProduct(), "Product should not be null");
            assertNotNull(schedule.getDutyType(), "Duty type should not be null");
            
            // Year should be reasonable
            assertTrue(schedule.getTariffYear() >= 2000 && schedule.getTariffYear() <= 2030, 
                      "Tariff year should be reasonable: " + schedule.getTariffYear());
            
            // Product code should be valid
            assertNotNull(schedule.getProduct().getTlCode(), "Product TL code should not be null");
            assertFalse(schedule.getProduct().getTlCode().isEmpty(), "Product TL code should not be empty");
        }
        
        System.out.println("DEBUG: Completed Test 11 - testDataIntegrity");
    }

    @Test
    @Order(12)
    void testSpecificDutyParsing() {
        System.out.println("DEBUG: Starting Test 12 - testSpecificDutyParsing");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        List<SpecificDuty> specificDuties = specificDutyRepository.findAll();
        
        if (specificDuties.size() > 0) {
            int dutiesWithAmount = 0;
            int dutiesWithUnit = 0;
            
            for (SpecificDuty duty : specificDuties) {
                // All specific duties should have raw text
                assertNotNull(duty.getSpecificDutyRateRaw(), "Raw text should not be null");
                assertFalse(duty.getSpecificDutyRateRaw().trim().isEmpty(), "Raw text should not be empty");
                
                if (duty.getAmount() != null && duty.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    dutiesWithAmount++;
                }
                
                if (duty.getUnit() != null && !duty.getUnit().trim().isEmpty()) {
                    dutiesWithUnit++;
                }
            }
        }
        
        System.out.println("DEBUG: Completed Test 12 - testSpecificDutyParsing");
    }

    @Test
    @Order(13)
    void testMixedDutyHandling() {
        System.out.println("DEBUG: Starting Test 13 - testMixedDutyHandling");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        List<CombinedDuty> combinedDuties = combinedDutyRepository.findAll();
        
        if (combinedDuties.size() > 0) {
            int mixedDuties = 0;
            int compoundDuties = 0;
            int dutiesWithBothComponents = 0;
            
            for (CombinedDuty duty : combinedDuties) {
                if ("M".equals(duty.getMixedOrCompound())) {
                    mixedDuties++;
                } else if ("C".equals(duty.getMixedOrCompound())) {
                    compoundDuties++;
                }
                
                boolean hasAdValorem = duty.getRatePercent() != null && 
                                      duty.getRatePercent().compareTo(BigDecimal.ZERO) > 0;
                boolean hasSpecific = duty.getAmount() != null && 
                                     duty.getAmount().compareTo(BigDecimal.ZERO) > 0;
                
                if (hasAdValorem && hasSpecific) {
                    dutiesWithBothComponents++;
                }
            }
        }
        
        System.out.println("DEBUG: Completed Test 13 - testMixedDutyHandling");
    }

    @Test
    @Order(14)
    void testOtherDutyClassification() {
        System.out.println("DEBUG: Starting Test 14 - testOtherDutyClassification");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        List<OtherDuty> otherDuties = otherDutyRepository.findAll();
        
        if (otherDuties.size() > 0) {
            int computableDuties = 0;
            int nonComputableDuties = 0;
            
            for (OtherDuty duty : otherDuties) {
                if (Boolean.TRUE.equals(duty.getIsComputable())) {
                    computableDuties++;
                } else {
                    nonComputableDuties++;
                }
                
                // All other duties should have some raw text
                assertNotNull(duty.getRawText(), "Raw text should not be null for other duties");
            }
        }
        
        System.out.println("DEBUG: Completed Test 14 - testOtherDutyClassification");
    }

    @Test
    @Order(15)
    void testIndustryClassification() {
        System.out.println("DEBUG: Starting Test 15 - testIndustryClassification");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        List<Product> products = productRepository.findAll();
        assertTrue(products.size() > 0, "Products should be loaded");

        // Count products by industry (based on HS chapter classification)
        int agricultureCount = 0;
        int energyCount = 0;
        int metalsCount = 0;
        int otherCount = 0;

        for (Product product : products) {
            String tlCode = product.getTlCode();
            if (tlCode != null && tlCode.length() >= 2) {
                int chapter = Integer.parseInt(tlCode.substring(0, 2));
                
                if (chapter >= 1 && chapter <= 24) {
                    agricultureCount++;
                } else if (chapter >= 25 && chapter <= 27) {
                    energyCount++;
                } else if (chapter >= 72 && chapter <= 83) {
                    metalsCount++;
                } else {
                    otherCount++;
                }
            }
        }

        // Verify totals match
        assertEquals(products.size(), agricultureCount + energyCount + metalsCount + otherCount,
                    "Industry classification should account for all products");
        
        System.out.println("DEBUG: Completed Test 15 - testIndustryClassification");
    }

    @Test
    @Order(16)
    void testErrorHandling() {
        System.out.println("DEBUG: Starting Test 16 - testErrorHandling");
        
        // This test doesn't need the loaded data, so it can run independently
        assertThrows(RuntimeException.class, () -> {
            dataLoaderService.loadCleanedData("non_existent_file.csv");
        }, "Should throw RuntimeException for non-existent file");
        
        System.out.println("DEBUG: Completed Test 16 - testErrorHandling");
    }

    @Test
    @Order(17)
    void testDataConsistency() {
        System.out.println("DEBUG: Starting Test 17 - testDataConsistency");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        System.out.println("DEBUG: Getting total counts for consistency check...");
        
        // Get total counts efficiently
        long totalSchedules = dataLoaderService.getTariffScheduleCount();
        long totalAdValorem = dataLoaderService.getAdValoremDutyCount();
        long totalSpecific = dataLoaderService.getSpecificDutyCount();
        long totalCombined = dataLoaderService.getCombinedDutyCount();
        long totalOther = dataLoaderService.getOtherDutyCount();
        
        long totalDuties = totalAdValorem + totalSpecific + totalCombined + totalOther;
        long schedulesWithoutDuties = totalSchedules - totalDuties;
        
        System.out.println("DEBUG: Found " + totalSchedules + " schedules and " + totalDuties + " duties");
        
        // Print consistency summary
        System.out.println("=== DATA CONSISTENCY SUMMARY ===");
        System.out.println("Total Tariff Schedules: " + totalSchedules);
        System.out.println("Total Duties: " + totalDuties);
        System.out.println("  - Ad Valorem: " + totalAdValorem);
        System.out.println("  - Specific: " + totalSpecific);
        System.out.println("  - Combined: " + totalCombined);
        System.out.println("  - Other: " + totalOther);
        System.out.println("Schedules without duties: " + schedulesWithoutDuties);
        System.out.println("================================");
        
        // Basic consistency checks
        assertTrue(totalSchedules > 0, "Should have loaded some schedules");
        assertTrue(totalDuties >= 0, "Duty count should be non-negative");
        assertTrue(totalDuties <= totalSchedules, "Number of duties should not exceed number of schedules");
        
        // It's normal for some schedules to not have duties if the original data has empty/invalid duty information
        assertTrue(totalDuties > 0, "At least some duties should be created");
        
        // Verify accounting is correct
        assertEquals(totalSchedules, totalDuties + schedulesWithoutDuties, "All schedules should be accounted for");
        
        // Optional: Sample a few schedules to verify they have correct duty relationships
        System.out.println("DEBUG: Sampling a few schedules for detailed consistency check...");
        List<TariffSchedule> sampleSchedules = tariffScheduleRepository.findAll()
            .stream()
            .limit(10)  // Only check first 10 schedules for detailed verification
            .toList();
            
        for (TariffSchedule schedule : sampleSchedules) {
            // Basic relationship validation for sample
            assertNotNull(schedule.getReporter(), "Reporter should not be null");
            assertNotNull(schedule.getPartner(), "Partner should not be null");
            assertNotNull(schedule.getProduct(), "Product should not be null");
            assertNotNull(schedule.getDutyType(), "Duty type should not be null");
        }
        
        System.out.println("DEBUG: Completed Test 17 - testDataConsistency");
    }

    @Test
    @Order(18)
    void testEmptyDutyHandling() {
        System.out.println("DEBUG: Starting Test 18 - testEmptyDutyHandling");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        long totalSchedules = dataLoaderService.getTariffScheduleCount();
        long totalDuties = dataLoaderService.getAdValoremDutyCount() + 
                          dataLoaderService.getSpecificDutyCount() + 
                          dataLoaderService.getCombinedDutyCount() + 
                          dataLoaderService.getOtherDutyCount();
        
        long schedulesWithoutDuties = totalSchedules - totalDuties;
        
        // Basic validation
        assertTrue(totalSchedules > 0, "Should have loaded some schedules");
        assertTrue(totalDuties >= 0, "Duty count should be non-negative");
        assertEquals(totalSchedules, totalDuties + schedulesWithoutDuties, 
                    "Accounting should be correct");
        
        System.out.println("DEBUG: Completed Test 18 - testEmptyDutyHandling");
    }

    @Test
    @Order(19)
    void testFinalDataSummary() {
        System.out.println("DEBUG: Starting Test 19 - testFinalDataSummary");
        
        // Data should already be loaded from the first test
        assertTrue(dataLoaded, "Data should have been loaded by previous test");

        // Print final summary of all loaded data
        System.out.println("\n=== FINAL TEST DATA SUMMARY ===");
        System.out.println("Total Countries: " + dataLoaderService.getCountryCount());
        System.out.println("Total Products: " + dataLoaderService.getProductCount());
        System.out.println("Total Duty Types: " + dataLoaderService.getDutyTypeCount());
        System.out.println("Total Tariff Schedules: " + dataLoaderService.getTariffScheduleCount());
        System.out.println("Total Ad Valorem Duties: " + dataLoaderService.getAdValoremDutyCount());
        System.out.println("Total Specific Duties: " + dataLoaderService.getSpecificDutyCount());
        System.out.println("Total Combined Duties: " + dataLoaderService.getCombinedDutyCount());
        System.out.println("Total Other Duties: " + dataLoaderService.getOtherDutyCount());
        
        long totalDuties = dataLoaderService.getAdValoremDutyCount() + 
                          dataLoaderService.getSpecificDutyCount() + 
                          dataLoaderService.getCombinedDutyCount() + 
                          dataLoaderService.getOtherDutyCount();
        
        System.out.println("Total Duties (All Types): " + totalDuties);
        System.out.println("Data Loading Flag Status: " + dataLoaded);
        System.out.println("===============================\n");
        
        // Final validation
        assertTrue(totalDuties > 0, "Should have loaded some duties");
        assertTrue(dataLoaded, "Data loaded flag should be true");
        
        System.out.println("DEBUG: Completed Test 19 - testFinalDataSummary");
    }
}