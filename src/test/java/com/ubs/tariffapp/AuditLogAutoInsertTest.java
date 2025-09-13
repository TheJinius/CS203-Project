package com.ubs.tariffapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;

import com.ubs.tariffapp.models.AuditLog;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.Duty;
import com.ubs.tariffapp.testutils.TestEntityFactory;
import com.ubs.tariffapp.repositories.AuditLogRepository;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.DutyTypeRepository;
import com.ubs.tariffapp.repositories.ProductRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.repositories.duty.DutyRepository;
import com.ubs.tariffapp.audit.AuditLogService;
import com.ubs.tariffapp.audit.AuditListener;

@DataJpaTest
@Import({AuditLogService.class, AuditListener.class}) // Import AuditLogService for dependency injection
@ActiveProfiles("test") // loads application-test.properties
public class AuditLogAutoInsertTest {
    @Autowired
    private DutyRepository dutyRepository;

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

    // ========================
    // TariffSchedule Tests
    // ========================

    @Test
    void testAuditLogCreatedForScheduleInsert() {
        Duty duty = TestEntityFactory.createNoneDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository, productRepository, dutyTypeRepository, scheduleRepository, duty);

        AuditLog log = auditLogRepository.findAll().stream()
                .filter(l -> l.getTariffSchedule().equals(schedule))
                .findFirst()
                .orElse(null);

        assertThat(log).isNotNull();
        assertThat(log.getChangeType()).isEqualTo("INSERT");
    }

    @Test
    void testAuditLogCreatedForScheduleUpdate() {
        Duty duty = TestEntityFactory.createNoneDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository, productRepository, dutyTypeRepository, scheduleRepository, duty);

        // Update the schedule
        schedule.setNote("Updated note");
        scheduleRepository.save(schedule);

        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> l.getTariffSchedule().equals(schedule))
                .toList();

        assertThat(logs).hasSizeGreaterThanOrEqualTo(2); // INSERT + UPDATE
        assertThat(logs.get(logs.size() - 1).getChangeType()).isEqualTo("UPDATE");
    }

    @Test
    void testAuditLogCreatedForScheduleDelete() {
        Duty duty = TestEntityFactory.createNoneDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository, productRepository, dutyTypeRepository, scheduleRepository, duty);

        scheduleRepository.delete(schedule);

        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> l.getTariffSchedule().equals(schedule))
                .toList();

        assertThat(logs).hasSizeGreaterThanOrEqualTo(2); // INSERT + DELETE
        assertThat(logs.get(logs.size() - 1).getChangeType()).isEqualTo("DELETE");
    }

    // ========================
    // Duty Tests
    // ========================

    @Test
    void testAuditLogCreatedForDutyInsert() {
        Duty duty = TestEntityFactory.createNoneDuty();

        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository, productRepository, dutyTypeRepository, scheduleRepository, null);

        duty.setTariffSchedule(schedule);
        schedule.setDuty(duty); // Optional to maintain in-memory bidirectional consistency

        dutyRepository.save(duty);

        AuditLog log = auditLogRepository.findAll().stream()
                .filter(l -> l.getTariffSchedule().equals(schedule))
                .findFirst()
                .orElse(null);

        assertThat(log).isNotNull();
        assertThat(log.getChangeType()).isEqualTo("INSERT");
    }

    @Test
    void testAuditLogCreatedForDutyUpdate() {
        Duty duty = TestEntityFactory.createNoneDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository, productRepository, dutyTypeRepository, scheduleRepository, null);

        duty.setTariffSchedule(schedule);
        schedule.setDuty(duty); // Optional to maintain in-memory bidirectional consistency

        dutyRepository.save(duty);

        // Update the duty
        duty.setMathExpression("updated expression");
        dutyRepository.save(duty);

        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> l.getTariffSchedule().equals(schedule))
                .toList();

        assertThat(logs).hasSizeGreaterThanOrEqualTo(2); // INSERT + UPDATE
        assertThat(logs.get(logs.size() - 1).getChangeType()).isEqualTo("UPDATE");
    }

    @Test
    void testAuditLogCreatedForDutyDelete() {
        Duty duty = TestEntityFactory.createNoneDuty();
        TariffSchedule schedule = TestEntityFactory.createAndSaveTariffSchedule(
                countryRepository, productRepository, dutyTypeRepository, scheduleRepository, null);

        duty.setTariffSchedule(schedule);
        schedule.setDuty(duty); // Optional to maintain in-memory bidirectional consistency

        dutyRepository.save(duty);

        dutyRepository.delete(duty);

        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> l.getTariffSchedule().equals(schedule))
                .toList();

        assertThat(logs).hasSizeGreaterThanOrEqualTo(2); // INSERT + DELETE
        assertThat(logs.get(logs.size() - 1).getChangeType()).isEqualTo("DELETE");
    }

}
