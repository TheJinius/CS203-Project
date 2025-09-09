package com.ubs.tariffapp.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.ubs.tariffapp.models.AdValoremDuty;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.testutils.TestEntityFactory;

@DataJpaTest
public class AdValoremDutyRepositoryTest {

    @Autowired
    private AdValoremDutyRepository dutyRepository;

    @Autowired
    private TariffScheduleRepository scheduleRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DutyTypeRepository dutyTypeRepository;

    @Test
    void testSaveAndFindById() {
        // Create and persist all required entities, including TariffSchedule
        AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
        TariffSchedule schedule = TestEntityFactory.createAndPersistTariffSchedule(
            countryRepository, 
            productRepository, 
            dutyTypeRepository,
            scheduleRepository, 
            duty
        );

        // Retrieve and assert
        Integer generatedId = schedule.getTariffId();
        AdValoremDuty found = dutyRepository.findById(generatedId).orElse(null);
		
        assertThat(found).isNotNull();
        assertThat(found.getTariffId()).isEqualTo(generatedId);
        assertThat(found.getRatePercent()).isEqualByComparingTo(duty.getRatePercent());
        assertThat(found.getMathExpression()).isEqualTo(duty.getMathExpression());
        assertThat(found.getDutyNature()).isEqualTo(duty.getDutyNature());
        assertThat(found.getTariffSchedule()).isNotNull(); // Only check if TariffSchedule is not null
        // We will check the TariffSchedule's fields in the TariffScheduleRepositoryTest instead
    }

    @Test
    void testDelete() {
        AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
        TariffSchedule schedule = TestEntityFactory.createAndPersistTariffSchedule(
            countryRepository, productRepository, dutyTypeRepository, scheduleRepository, duty
        );

        Integer generatedId = schedule.getTariffId();
        dutyRepository.deleteById(generatedId);

        assertThat(dutyRepository.findById(generatedId)).isEmpty();
    }
}