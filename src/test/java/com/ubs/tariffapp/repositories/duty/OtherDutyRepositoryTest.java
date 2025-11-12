package com.ubs.tariffapp.repositories.duty;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.OtherDuty;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.DutyTypeRepository;
import com.ubs.tariffapp.repositories.ProductRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.testutils.TestEntityFactory;

@DataJpaTest
@ActiveProfiles("test") // loads application-test.properties
@ExtendWith(DockerRequiredExtension.class)
public class OtherDutyRepositoryTest {

    @Autowired
    private OtherDutyRepository dutyRepository;

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
        OtherDuty duty = TestEntityFactory.createOtherDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
            countryRepository,
            productRepository,
            dutyTypeRepository,
            scheduleRepository,
            duty
        );

        Integer generatedId = schedule.getTariffId();
        OtherDuty found = dutyRepository.findById(generatedId).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getTariffId()).isEqualTo(generatedId);
        assertThat(found.getDutyNature()).isEqualTo(duty.getDutyNature());
        assertThat(found.getMathExpression()).isEqualTo(duty.getMathExpression());
        assertThat(found.getRawText()).isEqualTo(duty.getRawText());
        assertThat(found.getIsComputable()).isEqualTo(duty.getIsComputable());
        assertThat(found.getTariffSchedule()).isNotNull(); 
        // Only check if TariffSchedule is not null
    }

    @Test
    void testDelete() {
        OtherDuty duty = TestEntityFactory.createOtherDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
            countryRepository,
            productRepository,
            dutyTypeRepository,
            scheduleRepository,
            duty
        );

        Integer generatedId = schedule.getTariffId();
        dutyRepository.deleteById(generatedId);

        assertThat(dutyRepository.findById(generatedId)).isEmpty();
    }
}
