package com.ubs.tariffapp.repositories;


import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

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
        Duty duty = TestEntityFactory.createAdValoremDuty();

        TariffSchedule schedule = TestEntityFactory.createAndPersistTariffSchedule(
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
        assertThat(found.getDuty()).isEqualTo(schedule.getDuty()); // Check for cascade persistence for Duty entity

        // Test for this in AdvaloremDutyRepositoryTest instead
        // assertThat(found.getDuty().getDutyNature()).isEqualTo(schedule.getDuty().getDutyNature());
        // assertThat(found.getDuty().getMathExpression()).isEqualTo(schedule.getDuty().getMathExpression());
        // assertThat(((AdValoremDuty) found.getDuty()).getRatePercent()).isEqualByComparingTo(schedule.getDuty().getRatePercent());
    }

    @Test
    void testDelete() {
        Duty duty = TestEntityFactory.createAdValoremDuty();

        TariffSchedule schedule = TestEntityFactory.createAndPersistTariffSchedule(
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
}
