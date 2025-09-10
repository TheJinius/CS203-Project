package com.ubs.tariffapp.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.testutils.TestEntityFactory;

@DataJpaTest
public class SpecificDutyRepositoryTest {

    @Autowired
    private SpecificDutyRepository dutyRepository;

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
        SpecificDuty duty = TestEntityFactory.createSpecificDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository,
                productRepository,
                dutyTypeRepository,
                scheduleRepository,
                duty);

        Integer generatedId = schedule.getTariffId();
        SpecificDuty found = dutyRepository.findById(generatedId).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getTariffId()).isEqualTo(generatedId);
        assertThat(found.getDutyNature()).isEqualTo(duty.getDutyNature());
        assertThat(found.getMathExpression()).isEqualTo(duty.getMathExpression());
        assertThat(found.getAmount()).isEqualByComparingTo(duty.getAmount());
        assertThat(found.getUnit()).isEqualTo(duty.getUnit());
        assertThat(found.getMultiplier()).isEqualTo(duty.getMultiplier());
        assertThat(found.getSpecificDutyRateRaw()).isEqualTo(duty.getSpecificDutyRateRaw());

        assertThat(found.getTariffSchedule()).isNotNull();
        // Only check if TariffSchedule is not null
    }

    @Test
    void testDelete() {
        SpecificDuty duty = TestEntityFactory.createSpecificDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository,
                productRepository,
                dutyTypeRepository,
                scheduleRepository,
                duty);

        Integer generatedId = schedule.getTariffId();
        dutyRepository.deleteById(generatedId);

        assertThat(dutyRepository.findById(generatedId)).isEmpty();
    }
}
