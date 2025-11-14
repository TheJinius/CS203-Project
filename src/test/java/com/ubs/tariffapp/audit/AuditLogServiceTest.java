package com.ubs.tariffapp.audit;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ubs.tariffapp.extensions.DockerRequiredExtension;
import com.ubs.tariffapp.models.AuditLog;
import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.DutyTypeId;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.Duty;
import com.ubs.tariffapp.repositories.AuditLogRepository;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.DutyTypeRepository;
import com.ubs.tariffapp.repositories.ProductRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.repositories.duty.DutyRepository;

// A test for the AuditLogService class in com.ubs.tariffapp.audit package
// Does not use TestEntityFactory because we need full control over flushing and entity relationships

@SpringBootTest // Use full Spring Boot context instead of @DataJpaTest
@ActiveProfiles("test")
@Transactional // Each test runs in its own transaction (rollback by default)
@ExtendWith(DockerRequiredExtension.class)
public class AuditLogServiceTest {

        @Autowired
        private TariffScheduleRepository scheduleRepository;

        @Autowired
        private DutyRepository dutyRepository;

        @Autowired
        private AuditLogRepository auditLogRepository;

        @Autowired
        private AuditLogService auditLogService;

        @Autowired
        private CountryRepository countryRepository;

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private DutyTypeRepository dutyTypeRepository;

        @Test
        void testAuditLogCreatedForScheduleInsert() {
                // Create a complete TariffSchedule with all required entities
                TariffSchedule schedule = createCompleteSchedule();
                scheduleRepository.save(schedule);

                // Flush to ensure database operations complete
                scheduleRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(schedule, "INSERT");

                // Check for audit log
                List<AuditLog> logs = auditLogRepository.findAll().stream()
                                .filter(log -> log.getTariffId().equals(schedule.getTariffId()))
                                .toList();

                assertThat(logs).hasSizeGreaterThanOrEqualTo(1);
                AuditLog insertLog = logs.stream()
                                .filter(log -> "INSERT".equals(log.getChangeType()))
                                .findFirst()
                                .orElse(null);

                assertThat(insertLog).isNotNull();
                assertThat(insertLog.getChangeType()).isEqualTo("INSERT");
        }

        @Test
        void testAuditLogCreatedForScheduleUpdate() {
                // Create and save a TariffSchedule
                TariffSchedule schedule = createCompleteSchedule();
                scheduleRepository.save(schedule);
                scheduleRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(schedule, "INSERT");

                // Update the schedule
                schedule.setNote("Updated note for testing");
                scheduleRepository.save(schedule);
                scheduleRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(schedule, "UPDATE");

                // Check for audit logs
                List<AuditLog> logs = auditLogRepository.findAll().stream()
                                .filter(log -> log.getTariffId().equals(schedule.getTariffId()))
                                .toList();

                assertThat(logs).hasSizeGreaterThanOrEqualTo(2); // Should have INSERT + UPDATE

                // Verify we have both INSERT and UPDATE logs
                boolean hasInsert = logs.stream().anyMatch(log -> "INSERT".equals(log.getChangeType()));
                boolean hasUpdate = logs.stream().anyMatch(log -> "UPDATE".equals(log.getChangeType()));

                assertThat(hasInsert).isTrue();
                assertThat(hasUpdate).isTrue();
        }

        @Test
        void testAuditLogCreatedForScheduleDelete() {
                // Create and save a TariffSchedule
                TariffSchedule schedule = createCompleteSchedule();
                scheduleRepository.save(schedule);
                scheduleRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(schedule, "INSERT");

                Integer scheduleId = schedule.getTariffId();

                // Delete the schedule
                scheduleRepository.delete(schedule);
                scheduleRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(schedule, "DELETE");

                // Check for audit logs (using the stored ID since entity is deleted)
                List<AuditLog> logs = auditLogRepository.findAll().stream()
                                .filter(log -> log.getTariffId().equals(scheduleId))
                                .toList();

                assertThat(logs).hasSizeGreaterThanOrEqualTo(2); // Should have INSERT + DELETE

                // Verify we have DELETE log
                boolean hasDelete = logs.stream().anyMatch(log -> "DELETE".equals(log.getChangeType()));
                assertThat(hasDelete).isTrue();
        }

        @Test
        void testAuditLogCreatedForDutyInsert() {
                // Create a schedule first (without duty)
                TariffSchedule schedule = createCompleteSchedule();
                scheduleRepository.save(schedule);
                scheduleRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(schedule, "INSERT");

                // Create and save a duty
                Duty duty = new Duty();
                duty.setTariffSchedule(schedule);
                duty.setDutyNature("NONE");
                duty.setMathExpression("0");

                dutyRepository.save(duty);
                dutyRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(duty, "INSERT");

                // Check for audit logs
                List<AuditLog> logs = auditLogRepository.findAll().stream()
                                .filter(log -> log.getTariffId().equals(schedule.getTariffId()))
                                .toList();

                // Should have logs for both schedule and duty
                assertThat(logs).hasSizeGreaterThanOrEqualTo(2);

                // Check for duty INSERT log
                boolean hasDutyInsert = logs.stream()
                                .anyMatch(log -> "INSERT".equals(log.getChangeType()) &&
                                                log.getChangeDetails() != null &&
                                                log.getChangeDetails().contains(duty.getClass().getSimpleName()));
                assertThat(hasDutyInsert).isTrue();
        }

        @Test
        void testAuditLogCreatedForDutyUpdate() {
                // Create a schedule and duty
                TariffSchedule schedule = createCompleteSchedule();
                scheduleRepository.save(schedule);
                scheduleRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(schedule, "INSERT");

                Duty duty = new Duty();
                duty.setTariffSchedule(schedule);
                duty.setDutyNature("NONE");
                duty.setMathExpression("0");

                dutyRepository.save(duty);
                dutyRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(duty, "INSERT");

                // Update the duty
                duty.setMathExpression("updated expression");
                dutyRepository.save(duty);
                dutyRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(duty, "UPDATE");

                // Check for audit logs
                List<AuditLog> logs = auditLogRepository.findAll().stream()
                                .filter(log -> log.getTariffId().equals(schedule.getTariffId()))
                                .toList();

                assertThat(logs).hasSizeGreaterThanOrEqualTo(3); // Schedule INSERT + Duty INSERT + Duty UPDATE

                // Check for duty UPDATE log
                boolean hasDutyUpdate = logs.stream()
                                .anyMatch(log -> "UPDATE".equals(log.getChangeType()) &&
                                                log.getChangeDetails() != null &&
                                                log.getChangeDetails().contains(duty.getClass().getSimpleName()));
                assertThat(hasDutyUpdate).isTrue();
        }

        @Test
        void testAuditLogCreatedForDutyDelete() {
                // Create a schedule and duty
                TariffSchedule schedule = createCompleteSchedule();
                scheduleRepository.save(schedule);
                scheduleRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(schedule, "INSERT");

                Duty duty = new Duty();
                duty.setTariffSchedule(schedule);
                duty.setDutyNature("NONE");
                duty.setMathExpression("0");

                dutyRepository.save(duty);
                dutyRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(duty, "INSERT");

                // Delete the duty
                dutyRepository.delete(duty);
                dutyRepository.flush();

                // Manually trigger audit logging to test the service
                auditLogService.logChange(duty, "DELETE");

                // Check for audit logs
                List<AuditLog> logs = auditLogRepository.findAll().stream()
                                .filter(log -> log.getTariffId().equals(schedule.getTariffId()))
                                .toList();

                assertThat(logs).hasSizeGreaterThanOrEqualTo(3); // Schedule INSERT + Duty INSERT + Duty DELETE

                // Check for duty DELETE log
                boolean hasDutyDelete = logs.stream()
                                .anyMatch(log -> "DELETE".equals(log.getChangeType()) &&
                                                log.getChangeDetails() != null &&
                                                log.getChangeDetails().contains(duty.getClass().getSimpleName()));
                assertThat(hasDutyDelete).isTrue();
        }

        /**
         * Helper method to create a complete TariffSchedule with all required entities
         */
        private TariffSchedule createCompleteSchedule() {
                // Create and save Country entities
                Country reporter = new Country();
                reporter.setCountryId("TR01");
                reporter.setCountryName("Test Reporter");
                reporter.setIsoCode("TR");
                countryRepository.saveAndFlush(reporter);

                Country partner = new Country();
                partner.setCountryId("TP01");
                partner.setCountryName("Test Partner");
                partner.setIsoCode("TP");
                countryRepository.saveAndFlush(partner);

                // Create and save Product
                Product product = new Product();
                product.setTlCode("123456");
                product.setDescription("Test Product");
                product.setDigits(6);
                productRepository.saveAndFlush(product);

                // Create and save DutyType
                DutyTypeId dutyTypeId = new DutyTypeId();
                dutyTypeId.setDutyType("MFN");
                dutyTypeId.setDutyCode("00");

                DutyType dutyType = new DutyType();
                dutyType.setId(dutyTypeId);
                dutyType.setDutyTypeDescription("Most Favoured Nation");
                dutyTypeRepository.saveAndFlush(dutyType);

                // Create TariffSchedule (don't save yet, return it for the caller to save)
                TariffSchedule schedule = new TariffSchedule();
                schedule.setReporter(reporter);
                schedule.setPartner(partner);
                schedule.setProduct(product);
                schedule.setDutyType(dutyType);
                schedule.setTariffYear(2025);
                schedule.setTlsSuffix("00");
                schedule.setNote("Test schedule");

                return schedule;
        }

        private Country createCountry(String code, String name) {
                Country country = new Country();
                country.setCountryId(code);
                country.setCountryName(name);
                return countryRepository.saveAndFlush(country);
        }

        @org.junit.jupiter.api.BeforeEach
        void clearAuditLogs() {
                // Clean up audit logs before each test since they use REQUIRES_NEW propagation
                auditLogRepository.deleteAll();
        }

        @AfterEach
        void clearSecurityContext() {
                SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("logChange() should use username claim from JWT token for username")
        void logChange_WithUsernameClaimInJwt_UsesUsername() {
                // Given
                Map<String, Object> headers = new HashMap<>();
                headers.put("alg", "RS256");

                Map<String, Object> claims = new HashMap<>();
                claims.put("username", "jwtuser");
                claims.put("sub", "user-id-123");

                Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
                JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(auth);

                Country country = createCountry("US", "United States");

                // When
                auditLogService.logChange(country, "INSERT");

                // Then
                List<AuditLog> logs = auditLogRepository.findAll();
                assertThat(logs).hasSize(1);
                assertThat(logs.get(0).getChangedBy()).isEqualTo("jwtuser");
        }

        @Test
        @DisplayName("logChange() should fallback to cognito:username claim when username is missing")
        void logChange_WithCognitoUsernameClaimInJwt_UsesCognitoUsername() {
                // Given
                Map<String, Object> headers = new HashMap<>();
                headers.put("alg", "RS256");

                Map<String, Object> claims = new HashMap<>();
                claims.put("cognito:username", "cognitouser");
                claims.put("sub", "user-id-456");

                Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
                JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(auth);

                Country country = createCountry("CA", "Canada");

                // When
                auditLogService.logChange(country, "INSERT");

                // Then
                List<AuditLog> logs = auditLogRepository.findAll();
                assertThat(logs).hasSize(1);
                assertThat(logs.get(0).getChangedBy()).isEqualTo("cognitouser");
        }

        @Test
        @DisplayName("logChange() should fallback to email claim when username and cognito:username are missing")
        void logChange_WithEmailClaimInJwt_UsesEmail() {
                // Given
                Map<String, Object> headers = new HashMap<>();
                headers.put("alg", "RS256");

                Map<String, Object> claims = new HashMap<>();
                claims.put("email", "user@example.com");
                claims.put("sub", "user-id-789");

                Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
                JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(auth);

                Country country = createCountry("UK", "United Kingdom");

                // When
                auditLogService.logChange(country, "INSERT");

                // Then
                List<AuditLog> logs = auditLogRepository.findAll();
                assertThat(logs).hasSize(1);
                assertThat(logs.get(0).getChangedBy()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("logChange() should fallback to preferred_username claim when other claims are missing")
        void logChange_WithPreferredUsernameClaimInJwt_UsesPreferredUsername() {
                // Given
                Map<String, Object> headers = new HashMap<>();
                headers.put("alg", "RS256");

                Map<String, Object> claims = new HashMap<>();
                claims.put("preferred_username", "preferreduser");
                claims.put("sub", "user-id-abc");

                Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
                JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(auth);

                Country country = createCountry("FR", "France");

                // When
                auditLogService.logChange(country, "INSERT");

                // Then
                List<AuditLog> logs = auditLogRepository.findAll();
                assertThat(logs).hasSize(1);
                assertThat(logs.get(0).getChangedBy()).isEqualTo("preferreduser");
        }

        @Test
        @DisplayName("logChange() should fallback to sub claim when all other claims are missing")
        void logChange_WithOnlySubClaimInJwt_UsesSub() {
                // Given
                Map<String, Object> headers = new HashMap<>();
                headers.put("alg", "RS256");

                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", "unique-user-id-xyz");

                Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), headers, claims);
                JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
                SecurityContextHolder.getContext().setAuthentication(auth);

                Country country = createCountry("DE", "Germany");

                // When
                auditLogService.logChange(country, "INSERT");

                // Then
                List<AuditLog> logs = auditLogRepository.findAll();
                assertThat(logs).hasSize(1);
                assertThat(logs.get(0).getChangedBy()).isEqualTo("unique-user-id-xyz");
        }

        @Test
        @DisplayName("logChange() should use getName() for non-JWT authentication")
        void logChange_WithNonJwtAuthentication_UsesName() {
                // Given
                TestingAuthenticationToken auth = new TestingAuthenticationToken("basicuser", "password");
                SecurityContextHolder.getContext().setAuthentication(auth);

                Country country = createCountry("JP", "Japan");

                // When
                auditLogService.logChange(country, "INSERT");

                // Then
                List<AuditLog> logs = auditLogRepository.findAll();
                assertThat(logs).hasSize(1);
                assertThat(logs.get(0).getChangedBy()).isEqualTo("basicuser");
        }

        @Test
        @DisplayName("logChange() should use 'system' when no authentication is present")
        void logChange_WithNoAuthentication_UsesSystem() {
                // Given
                SecurityContextHolder.clearContext();

                Country country = createCountry("AU", "Australia");

                // When
                auditLogService.logChange(country, "INSERT");

                // Then
                List<AuditLog> logs = auditLogRepository.findAll();
                assertThat(logs).hasSize(1);
                assertThat(logs.get(0).getChangedBy()).isEqualTo("system");
        }
}

