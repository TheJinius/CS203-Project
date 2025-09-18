package com.ubs.tariffapp.services;

import com.ubs.tariffapp.repositories.*;
import com.ubs.tariffapp.repositories.duty.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DataLoaderServiceTest {

    @Autowired
    private DataLoaderService dataLoaderService;

    @Autowired
    private TariffScheduleRepository tariffScheduleRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DutyTypeRepository dutyTypeRepository;

    @Autowired
    private AdValoremDutyRepository adValoremDutyRepository;

    @Autowired
    private SpecificDutyRepository specificDutyRepository;

    @Autowired
    private CombinedDutyRepository combinedDutyRepository;

    @Autowired
    private OtherDutyRepository otherDutyRepository;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        adValoremDutyRepository.deleteAll();
        specificDutyRepository.deleteAll();
        combinedDutyRepository.deleteAll();
        otherDutyRepository.deleteAll();
        tariffScheduleRepository.deleteAll();
        dutyTypeRepository.deleteAll();
        productRepository.deleteAll();
        countryRepository.deleteAll();
    }

    @Test
    void testLoadCleanedData() {
        // Arrange
        String testFileName = "clean_HS2017SGYear2023.csv";
        
        // Get initial counts
        long initialTariffCount = dataLoaderService.getTariffScheduleCount();
        long initialCountryCount = dataLoaderService.getCountryCount();
        long initialProductCount = dataLoaderService.getProductCount();

        // Act
        assertDoesNotThrow(() -> dataLoaderService.loadCleanedData(testFileName));

        // Assert
        long finalTariffCount = dataLoaderService.getTariffScheduleCount();
        long finalCountryCount = dataLoaderService.getCountryCount();
        long finalProductCount = dataLoaderService.getProductCount();

        assertTrue(finalTariffCount > initialTariffCount, "Tariff schedules should be loaded");
        assertTrue(finalCountryCount > initialCountryCount, "Countries should be loaded");
        assertTrue(finalProductCount > initialProductCount, "Products should be loaded");

        System.out.println("Loaded " + (finalTariffCount - initialTariffCount) + " tariff schedules");
        System.out.println("Loaded " + (finalCountryCount - initialCountryCount) + " countries");
        System.out.println("Loaded " + (finalProductCount - initialProductCount) + " products");
    }

    @Test
    void testLoadDataWithInvalidFile() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            dataLoaderService.loadCleanedData("non_existent_file.csv"));
        
        assertTrue(exception.getMessage().contains("not found"));
    }
}