package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.services.TariffScheduleService;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.duty.*;
import com.ubs.tariffapp.exceptions.DutyNotFoundException;
import com.ubs.tariffapp.exceptions.InvalidRequestException;
import com.ubs.tariffapp.exceptions.TariffNotFoundException;
import com.ubs.tariffapp.extensions.DockerRequiredExtension;
import com.ubs.tariffapp.testutils.TestEntityFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Arrays;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(DockerRequiredExtension.class)
public class DutyServiceTest {

    @Autowired
    private DutyService dutyService;
    
    @MockBean
    private TariffScheduleService tariffScheduleService;

    // =================== NEW METHOD TESTS ===================

    @Test
    void testSearchAvailableTariffs_Success() {
        // Arrange
        TariffSchedule tariff1 = createBasicTariffSchedule("USA", "CHN", "010121");
        TariffSchedule tariff2 = createBasicTariffSchedule("USA", "CHN", "010122");
        
        when(tariffScheduleService.searchTariffSchedules("USA", "CHN", "010121", 2023))
            .thenReturn(Arrays.asList(tariff1, tariff2));

        // Act
        List<TariffSchedule> result = dutyService.searchAvailableTariffs("USA", "CHN", "010121", 2023);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).contains(tariff1, tariff2);
    }

    @Test
    void testSearchAvailableTariffs_NotFound() {
        // Arrange
        when(tariffScheduleService.searchTariffSchedules("XXX", "YYY", "NOTFOUND", 2023))
            .thenReturn(List.of()); // Empty list

        // Act & Assert
        TariffNotFoundException exception = assertThrows(TariffNotFoundException.class, () -> {
            dutyService.searchAvailableTariffs("XXX", "YYY", "NOTFOUND", 2023);
        });

        assertThat(exception.getMessage()).contains("No tariff schedules found");
    }

    @Test
    void testCalculateTariffById_AdValoremDuty() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(10.0); // 10%
        tariffSchedule.setTariffId(12345);

        when(tariffScheduleService.getTariffScheduleById(12345))
            .thenReturn(tariffSchedule);

        // Act
        double result = dutyService.calculateTariffById(12345, 1000.0);

        // Assert - 10% of $1000 = $100
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    void testCalculateTariffById_SpecificDuty() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithSpecificDuty(5.0, 1); // $5 per unit
        tariffSchedule.setTariffId(12346);

        when(tariffScheduleService.getTariffScheduleById(12346))
            .thenReturn(tariffSchedule);

        // Act
        double result = dutyService.calculateTariffById(12346, 100.0);

        // Assert - (100 / 1) * $5 = $500
        assertThat(result).isEqualTo(500.0);
    }

    @Test
    void testCalculateTariffById_CombinedDuty_Compound() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithCombinedDuty(5.0, 2.0, 1, "C");
        tariffSchedule.setTariffId(12347);

        when(tariffScheduleService.getTariffScheduleById(12347))
            .thenReturn(tariffSchedule);

        // Act
        double result = dutyService.calculateTariffById(12347, 50.0);

        // Assert - AdValorem: (50 * 5%) + Specific: (50/1 * $2) = $2.5 + $100 = $102.5
        assertThat(result).isEqualTo(102.5);
    }

    @Test
    void testCalculateTariffById_CombinedDuty_Mixed() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithCombinedDuty(2.0, 10.0, 1, "M");
        tariffSchedule.setTariffId(12348);

        when(tariffScheduleService.getTariffScheduleById(12348))
            .thenReturn(tariffSchedule);

        // Act
        double result = dutyService.calculateTariffById(12348, 20.0);

        // Assert - AdValorem: (20 * 2% = $0.4), Specific: (20/1 * $10 = $200)
        // Max($0.4, $200) = $200
        assertThat(result).isEqualTo(200.0);
    }

    @Test
    void testCalculateTariffById_TariffNotFound() {
        // Arrange
        when(tariffScheduleService.getTariffScheduleById(99999))
            .thenReturn(null);

        // Act & Assert
        TariffNotFoundException exception = assertThrows(TariffNotFoundException.class, () -> {
            dutyService.calculateTariffById(99999, 1000.0);
        });

        assertThat(exception.getMessage()).contains("No tariff schedule found for ID: 99999");
    }

    @Test
    void testCalculateTariffById_NoDuty() {
        // Arrange
        TariffSchedule tariffSchedule = createBasicTariffSchedule("USA", "CHN", "010121");
        tariffSchedule.setTariffId(12349);
        tariffSchedule.setDuty(null); // No duty attached

        when(tariffScheduleService.getTariffScheduleById(12349))
            .thenReturn(tariffSchedule);

        // Act & Assert
        DutyNotFoundException exception = assertThrows(DutyNotFoundException.class, () -> {
            dutyService.calculateTariffById(12349, 1000.0);
        });

        assertThat(exception.getMessage()).contains("No duty information found for TariffSchedule ID: 12349");
    }

    @Test
    void testCalculateTariffById_InvalidAmount() {
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            dutyService.calculateTariffById(12345, -100.0);
        });

        assertThat(exception.getMessage()).contains("Amount of product must be greater than 0");
    }

    @Test
    void testCalculateTariffById_NullTariffId() {
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            dutyService.calculateTariffById(null, 1000.0);
        });

        assertThat(exception.getMessage()).contains("Tariff ID is required");
    }

    // =================== EXISTING METHOD TESTS (Updated) ===================

    @Test
    void testCalculateTariff_AdValoremDuty() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(10.0);
        
        // Mock the method WITH year parameter (since calculateTariff() defaults to 2023)
        when(tariffScheduleService.getTariffSchedule("USA", "CHN", "010121", 2023))
            .thenReturn(tariffSchedule);

        // Act
        double result = dutyService.calculateTariff("USA", "CHN", "010121", 1000.0);

        // Assert
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    void testCalculateTariff_WithYear() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(5.0);
        
        when(tariffScheduleService.getTariffSchedule("USA", "CHN", "010121", 2023))
            .thenReturn(tariffSchedule);

        // Act
        double result = dutyService.calculateTariff("USA", "CHN", "010121", 1000.0, 2023);

        // Assert
        assertThat(result).isEqualTo(50.0);
    }

    @Test
    void testCalculateTariff_RoundingBehavior() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(7.333);
        
        // Mock the method WITH year parameter (since calculateTariff() defaults to 2023)
        when(tariffScheduleService.getTariffSchedule("USA", "JPN", "010200", 2023))
            .thenReturn(tariffSchedule);

        // Act
        double result = dutyService.calculateTariff("USA", "JPN", "010200", 133.33);

        // Assert - Should be rounded to 2 decimal places
        assertThat(result).isEqualTo(9.78);
    }

    @Test
    void testCalculateTariff_NoDutyFound() {
        // Arrange - Mock the method WITH year parameter
        when(tariffScheduleService.getTariffSchedule("XXX", "YYY", "NOTFOUND", 2023))
            .thenReturn(null);

        // Act & Assert
        TariffNotFoundException exception = assertThrows(TariffNotFoundException.class, () -> {
            dutyService.calculateTariff("XXX", "YYY", "NOTFOUND", 1000.0);
        });

        assertThat(exception.getMessage()).contains("No tariff schedule found");
    }

    @Test
    void testCalculateTariff_ServiceNotNull() {
        assertThat(dutyService).isNotNull();
    }

    // =================== HELPER METHODS ===================
    // Now using TestEntityFactory for consistent test data creation

    private TariffSchedule createBasicTariffSchedule(String reporterCode, String partnerCode, String productCode) {
        Country reporter = TestEntityFactory.createReporterCountry();
        reporter.setCountryId(reporterCode);
        
        Country partner = TestEntityFactory.createPartnerCountry();
        partner.setCountryId(partnerCode);
        
        Product product = TestEntityFactory.createProduct();
        product.setTlCode(productCode);
        
        AdValoremDuty duty = TestEntityFactory.createAdValoremDuty();
        TariffSchedule tariffSchedule = TestEntityFactory.createTariffSchedule(
            reporter, partner, product, TestEntityFactory.createDutyType(), duty);
        
        return tariffSchedule;
    }

    private TariffSchedule createTariffWithAdValoremDuty(double ratePercent) {
        Country reporter = TestEntityFactory.createReporterCountry();
        Country partner = TestEntityFactory.createPartnerCountry();
        Product product = TestEntityFactory.createProduct();
        
        AdValoremDuty adValoremDuty = TestEntityFactory.createAdValoremDuty();
        adValoremDuty.setRatePercent(BigDecimal.valueOf(ratePercent));
        
        TariffSchedule tariffSchedule = TestEntityFactory.createTariffSchedule(
            reporter, partner, product, TestEntityFactory.createDutyType(), adValoremDuty);
        
        return tariffSchedule;
    }

    private TariffSchedule createTariffWithSpecificDuty(double amount, int multiplier) {
        Country reporter = TestEntityFactory.createReporterCountry();
        Country partner = TestEntityFactory.createPartnerCountry();
        Product product = TestEntityFactory.createProduct();
        
        SpecificDuty specificDuty = TestEntityFactory.createSpecificDuty();
        specificDuty.setAmount(BigDecimal.valueOf(amount));
        specificDuty.setMultiplier(multiplier);
        
        TariffSchedule tariffSchedule = TestEntityFactory.createTariffSchedule(
            reporter, partner, product, TestEntityFactory.createDutyType(), specificDuty);
        
        return tariffSchedule;
    }

    private TariffSchedule createTariffWithCombinedDuty(double ratePercent, double amount, int multiplier, String mixedOrConditional) {
        Country reporter = TestEntityFactory.createReporterCountry();
        Country partner = TestEntityFactory.createPartnerCountry();
        Product product = TestEntityFactory.createProduct();
        
        CombinedDuty combinedDuty = TestEntityFactory.createCombinedDuty();
        combinedDuty.setRatePercent(BigDecimal.valueOf(ratePercent));
        combinedDuty.setAmount(BigDecimal.valueOf(amount));
        combinedDuty.setMultiplier(multiplier);
        combinedDuty.setMixedOrCompound(mixedOrConditional);
        
        TariffSchedule tariffSchedule = TestEntityFactory.createTariffSchedule(
            reporter, partner, product, TestEntityFactory.createDutyType(), combinedDuty);
        
        return tariffSchedule;
    }
}
