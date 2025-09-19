package com.ubs.tariffapp.services;

import com.ubs.tariffapp.models.*;
import com.ubs.tariffapp.repositories.*;
import com.ubs.tariffapp.repositories.duty.*;
import com.ubs.tariffapp.services.DataLoaderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
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
        String testFileName = "clean_HS2017USAYear2023.csv";
        System.out.println("Testing with file: " + testFileName);

        // Get initial counts
        long initialTariffCount = dataLoaderService.getTariffScheduleCount();
        long initialCountryCount = dataLoaderService.getCountryCount();
        long initialProductCount = dataLoaderService.getProductCount();

        // Act
        assertDoesNotThrow(() -> dataLoaderService.loadCleanedData(testFileName));

        // Debug: Print counts after loading
        long finalTariffCount = dataLoaderService.getTariffScheduleCount();
        long finalCountryCount = dataLoaderService.getCountryCount();
        long finalProductCount = dataLoaderService.getProductCount();

        System.out.println("Initial Tariff Count: " + initialTariffCount);
        System.out.println("Final Tariff Count: " + finalTariffCount);
        System.out.println("Initial Country Count: " + initialCountryCount);
        System.out.println("Final Country Count: " + finalCountryCount);
        System.out.println("Initial Product Count: " + initialProductCount);
        System.out.println("Final Product Count: " + finalProductCount);

        // Debug: Print first few rows of loaded data
        System.out.println("\n=== First 3 Countries ===");
        countryRepository.findAll(PageRequest.of(0, 3)).forEach(country -> 
            System.out.println("Country: " + country.getCountryId() + " - " + country.getCountryName()));

        System.out.println("\n=== First 3 Products ===");
        productRepository.findAll(PageRequest.of(0, 3)).forEach(product -> 
            System.out.println("Product: " + product.getTlCode() + " - " + product.getDescription()));

        System.out.println("\n=== First 3 Tariff Schedules ===");
        tariffScheduleRepository.findAll(PageRequest.of(0, 3)).forEach(tariff -> 
            System.out.println("Tariff ID: " + tariff.getTariffId() + 
                " | Reporter: " + tariff.getReporter().getCountryId() + 
                " | Partner: " + tariff.getPartner().getCountryId() +
                " | Product: " + tariff.getProduct().getTlCode() + 
                " | Duty Nature: " + (tariff.getDuty() != null ? tariff.getDuty().getDutyNature() : "N/A")));

        // Assert
        assertTrue(finalTariffCount > initialTariffCount, 
            "Tariff schedules should be loaded. Initial: " + initialTariffCount + ", Final: " + finalTariffCount);
        assertTrue(finalCountryCount > initialCountryCount, 
            "Countries should be loaded. Initial: " + initialCountryCount + ", Final: " + finalCountryCount);
        assertTrue(finalProductCount > initialProductCount, 
            "Products should be loaded. Initial: " + initialProductCount + ", Final: " + finalProductCount);

        System.out.println("Loaded " + (finalTariffCount - initialTariffCount) + " tariff schedules");
        System.out.println("Loaded " + (finalCountryCount - initialCountryCount) + " countries");
        System.out.println("Loaded " + (finalProductCount - initialProductCount) + " products");
    }

    @Test
    void testLoadDataWithInvalidFile() {
        String invalidFileName = "non_existent_file.csv";
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            dataLoaderService.loadCleanedData(invalidFileName));
        
        assertTrue(exception.getMessage().contains("Cleaned data file not found: " + invalidFileName), 
            "Exception message should indicate the file was not found. Actual: " + exception.getMessage());
    }
}