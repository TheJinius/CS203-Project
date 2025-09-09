package com.ubs.tariffapp.repositories;


import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.Duty;
import com.ubs.tariffapp.testutils.TestEntityFactory;


@DataJpaTest
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
        // Parent Java objects do not have their lists updated in memory when children
        // are added
        // But the database relationships are correctly established
        Country reporter = TestEntityFactory.createReporterCountry();
        Country partner = TestEntityFactory.createPartnerCountry();
        Product product = TestEntityFactory.createProduct();
        DutyType dutyType = TestEntityFactory.createDutyType();

        // Persist related entities first
        countryRepository.save(reporter);
        countryRepository.save(partner);
        productRepository.save(product);
        dutyTypeRepository.save(dutyType);

        // No need to persist Duty, it will be cascaded (see TariffSchedule model):

        Duty duty = TestEntityFactory.createAdValoremDuty();

        TariffSchedule schedule = TestEntityFactory.createTariffSchedule(
                reporter,
                partner,
                product,
                dutyType,
                duty
        );

        repository.save(schedule);

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
        assertThat(found.getDuty()).isEqualTo(schedule.getDuty()); // Check for cascade persistence for Duty entity

        // Test for this in AdvaloremDutyRepositoryTest instead
        // assertThat(found.getDuty().getDutyNature()).isEqualTo(schedule.getDuty().getDutyNature());
        // assertThat(found.getDuty().getMathExpression()).isEqualTo(schedule.getDuty().getMathExpression());
        // assertThat(((AdValoremDuty) found.getDuty()).getRatePercent()).isEqualByComparingTo(schedule.getDuty().getRatePercent());
    }

    @Test
    void testDelete() {
        Country reporter = TestEntityFactory.createReporterCountry();
        Country partner = TestEntityFactory.createPartnerCountry();
        Product product = TestEntityFactory.createProduct();
        DutyType dutyType = TestEntityFactory.createDutyType();

        // Persist related entities first
        countryRepository.save(reporter);
        countryRepository.save(partner);
        productRepository.save(product);
        dutyTypeRepository.save(dutyType);

        Duty duty = TestEntityFactory.createAdValoremDuty();

        TariffSchedule schedule = TestEntityFactory.createTariffSchedule(
                reporter,
                partner,
                product,
                dutyType,
                duty
        );
        repository.save(schedule);

        Integer generatedId = schedule.getTariffId();

        repository.deleteById(generatedId);
        assertThat(repository.findById(generatedId)).isEmpty();
    }
}
