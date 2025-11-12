package com.ubs.tariffapp.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.Duty;
import com.ubs.tariffapp.testutils.TestEntityFactory;


@DataJpaTest
@ActiveProfiles("test") // loads application-test.properties
public class TariffScheduleRepositoryTest {

    @Autowired
    private TariffScheduleRepository repository;

    // Repositories for related entities (to persist them first)
    @Autowired
    private DutyTypeRepository dutyTypeRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testSaveAndFindById() {
        Duty duty = TestEntityFactory.createNoneDuty();

        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository,
                productRepository,
                dutyTypeRepository,
                repository,
                duty
        );

        Integer generatedId = schedule.getTariffId();

        TariffSchedule found = repository.findById(generatedId).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getTariffYear()).isEqualTo(schedule.getTariffYear());
        assertThat(found.getTlsSuffix()).isEqualTo(schedule.getTlsSuffix());
        assertThat(found.getNote()).isEqualTo(schedule.getNote());
        assertThat(found.getReporter().getCountryName()).isEqualTo(schedule.getReporter().getCountryName());
        assertThat(found.getPartner().getCountryName()).isEqualTo(schedule.getPartner().getCountryName());
        assertThat(found.getProduct().getDescription()).isEqualTo(schedule.getProduct().getDescription());
        assertThat(found.getDutyType().getDutyTypeDescription()).isEqualTo(schedule.getDutyType().getDutyTypeDescription());
        assertThat(found.getDuty()).isNotNull(); // Only check if duty is not null
        // We will check the duty's fields in the relevant subclasses repository tests
    }

    @Test
    void testDelete() {
        Duty duty = TestEntityFactory.createAdValoremDuty();

        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository,
                productRepository,
                dutyTypeRepository,
                repository,
                duty
        );

        Integer generatedId = schedule.getTariffId();

        repository.deleteById(generatedId);
        assertThat(repository.findById(generatedId)).isEmpty();
    }

    @Test
    void testFindDistinctYears() {
        // Create and save shared entities once
        Country reporter = TestEntityFactory.createReporterCountry();
        Country partner = TestEntityFactory.createPartnerCountry();
        DutyType dutyType = TestEntityFactory.createDutyType();
        
        countryRepository.saveAndFlush(reporter);
        countryRepository.saveAndFlush(partner);
        dutyTypeRepository.saveAndFlush(dutyType);

        // Create unique products for each schedule to avoid unique constraint violation
        Product product1 = new Product("10011000", "Product 1", 8, new java.util.ArrayList<>());
        Product product2 = new Product("10012000", "Product 2", 8, new java.util.ArrayList<>());
        Product product3 = new Product("10013000", "Product 3", 8, new java.util.ArrayList<>());
        Product product4 = new Product("10014000", "Product 4", 8, new java.util.ArrayList<>());
        
        productRepository.saveAndFlush(product1);
        productRepository.saveAndFlush(product2);
        productRepository.saveAndFlush(product3);
        productRepository.saveAndFlush(product4);

        // Create tariff with year 2023
        Duty duty1 = TestEntityFactory.createAdValoremDuty();
        TariffSchedule schedule1 = TestEntityFactory.createTariffSchedule(reporter, partner, product1, dutyType, duty1);
        schedule1.setTariffYear(2023);
        repository.saveAndFlush(schedule1);

        // Create tariff with year 2022
        Duty duty2 = TestEntityFactory.createSpecificDuty();
        TariffSchedule schedule2 = TestEntityFactory.createTariffSchedule(reporter, partner, product2, dutyType, duty2);
        schedule2.setTariffYear(2022);
        repository.saveAndFlush(schedule2);

        // Create another tariff with year 2023 (duplicate year, but different product)
        Duty duty3 = TestEntityFactory.createNoneDuty();
        TariffSchedule schedule3 = TestEntityFactory.createTariffSchedule(reporter, partner, product3, dutyType, duty3);
        schedule3.setTariffYear(2023);
        repository.saveAndFlush(schedule3);

        // Create tariff with year 2021
        Duty duty4 = TestEntityFactory.createAdValoremDuty();
        TariffSchedule schedule4 = TestEntityFactory.createTariffSchedule(reporter, partner, product4, dutyType, duty4);
        schedule4.setTariffYear(2021);
        repository.saveAndFlush(schedule4);

        // Test findDistinctYears
        List<Integer> distinctYears = repository.findDistinctYears();

        // Assert that we have exactly 3 distinct years
        assertThat(distinctYears).hasSize(3);
        
        // Assert that years are in descending order
        assertThat(distinctYears).containsExactly(2023, 2022, 2021);
        
        // Assert that duplicate year 2023 only appears once
        assertThat(distinctYears).containsOnlyOnce(2023);
    }
}
