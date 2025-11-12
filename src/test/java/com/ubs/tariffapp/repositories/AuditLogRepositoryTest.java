package com.ubs.tariffapp.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;
import com.ubs.tariffapp.models.AuditLog;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.Duty;
import com.ubs.tariffapp.testutils.TestEntityFactory;

@DataJpaTest
@ActiveProfiles("test") // loads application-test.properties
@ExtendWith(DockerRequiredExtension.class)
public class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

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
        // TODO: Test with generic Duty (after we make Duty a concrete class)
        Duty duty = TestEntityFactory.createAdValoremDuty();

        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository,
                productRepository,
                dutyTypeRepository,
                scheduleRepository,
                duty);

        // Create and save an AuditLog
        AuditLog log = TestEntityFactory.createAuditLog(schedule);
        auditLogRepository.save(log);

        Integer generatedId = log.getLogId();
        AuditLog found = auditLogRepository.findById(generatedId).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getLogId()).isEqualTo(generatedId);
        assertThat(found.getTariffId()).isEqualTo(schedule.getTariffId());
        assertThat(found.getChangeType()).isEqualTo(log.getChangeType());
        assertThat(found.getChangedBy()).isEqualTo(log.getChangedBy());
        assertThat(found.getChangeDate()).isEqualTo(log.getChangeDate());
        assertThat(found.getChangeDetails()).isEqualTo(log.getChangeDetails());
    }

    @Test
    void testDelete() {
        // TODO: Test with generic Duty (after we make Duty a concrete class)
        Duty duty = TestEntityFactory.createAdValoremDuty();

        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository,
                productRepository,
                dutyTypeRepository,
                scheduleRepository,
                duty);

        AuditLog log = TestEntityFactory.createAuditLog(schedule);
        auditLogRepository.save(log);

        Integer generatedId = log.getLogId();
        auditLogRepository.deleteById(generatedId);

        assertThat(auditLogRepository.findById(generatedId)).isEmpty();
    }
}
