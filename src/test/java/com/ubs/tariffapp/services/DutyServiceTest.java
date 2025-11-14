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

    // =================== OVERLOADED METHOD TESTS (with productValueDollars) ===================

    @Test
    void testCalculateTariffById_WithProductValue_CombinedDuty_Compound() {
        // Arrange
        // 5% ad valorem + $2 per unit
        TariffSchedule tariffSchedule = createTariffWithCombinedDuty(5.0, 2.0, 1, "C");
        tariffSchedule.setTariffId(20001);

        when(tariffScheduleService.getTariffScheduleById(20001))
            .thenReturn(tariffSchedule);

        // Act - Product worth $1000, quantity is 50 units
        double result = dutyService.calculateTariffById(20001, 50.0, 1000.0);

        // Assert - AdValorem: ($1000 * 5% = $50) + Specific: (50/1 * $2 = $100) = $150
        assertThat(result).isEqualTo(150.0);
    }

    @Test
    void testCalculateTariffById_WithProductValue_CombinedDuty_Mixed() {
        // Arrange
        // 10% ad valorem or $5 per unit, whichever is higher
        TariffSchedule tariffSchedule = createTariffWithCombinedDuty(10.0, 5.0, 1, "M");
        tariffSchedule.setTariffId(20002);

        when(tariffScheduleService.getTariffScheduleById(20002))
            .thenReturn(tariffSchedule);

        // Act - Product worth $100, quantity is 50 units
        double result = dutyService.calculateTariffById(20002, 50.0, 100.0);

        // Assert - AdValorem: ($100 * 10% = $10), Specific: (50/1 * $5 = $250)
        // Max($10, $250) = $250
        assertThat(result).isEqualTo(250.0);
    }

    @Test
    void testCalculateTariffById_WithProductValue_AdValoremDuty() {
        // Arrange - Should fall back to regular calculation since it's not CombinedDuty
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(15.0);
        tariffSchedule.setTariffId(20003);

        when(tariffScheduleService.getTariffScheduleById(20003))
            .thenReturn(tariffSchedule);

        // Act - Even though productValue is provided, it uses amountOfProduct for ad valorem
        double result = dutyService.calculateTariffById(20003, 500.0, 2000.0);

        // Assert - 15% of $500 = $75 (ignores the $2000 productValue)
        assertThat(result).isEqualTo(75.0);
    }

    @Test
    void testCalculateTariffById_WithProductValue_NullProductValue() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithCombinedDuty(5.0, 3.0, 1, "C");
        tariffSchedule.setTariffId(20004);

        when(tariffScheduleService.getTariffScheduleById(20004))
            .thenReturn(tariffSchedule);

        // Act - null productValue should fall back to legacy calculation
        double result = dutyService.calculateTariffById(20004, 100.0, null);

        // Assert - Legacy: uses 100 for both ad valorem and specific
        // AdValorem: (100 * 5% = $5) + Specific: (100/1 * $3 = $300) = $305
        assertThat(result).isEqualTo(305.0);
    }

    @Test
    void testCalculateTariffById_WithProductValue_InvalidAmount() {
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            dutyService.calculateTariffById(20005, 0.0, 1000.0);
        });

        assertThat(exception.getMessage()).contains("Amount of product must be greater than 0");
    }

    @Test
    void testCalculateTariffById_WithProductValue_NullTariffId() {
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            dutyService.calculateTariffById(null, 100.0, 1000.0);
        });

        assertThat(exception.getMessage()).contains("Tariff ID is required");
    }

    @Test
    void testCalculateTariffById_WithProductValue_TariffNotFound() {
        // Arrange
        when(tariffScheduleService.getTariffScheduleById(99998))
            .thenReturn(null);

        // Act & Assert
        TariffNotFoundException exception = assertThrows(TariffNotFoundException.class, () -> {
            dutyService.calculateTariffById(99998, 100.0, 1000.0);
        });

        assertThat(exception.getMessage()).contains("No tariff schedule found for ID: 99998");
    }

    @Test
    void testCalculateTariffById_WithProductValue_NoDuty() {
        // Arrange
        TariffSchedule tariffSchedule = createBasicTariffSchedule("USA", "CHN", "010121");
        tariffSchedule.setTariffId(20006);
        tariffSchedule.setDuty(null);

        when(tariffScheduleService.getTariffScheduleById(20006))
            .thenReturn(tariffSchedule);

        // Act & Assert
        DutyNotFoundException exception = assertThrows(DutyNotFoundException.class, () -> {
            dutyService.calculateTariffById(20006, 100.0, 1000.0);
        });

        assertThat(exception.getMessage()).contains("No duty information found for TariffSchedule ID: 20006");
    }

    // =================== CALCULATE WITH TARIFFSCHEDULE TESTS ===================

    @Test
    void testCalculateTariff_WithTariffSchedule_AdValorem() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(12.5);

        // Act
        double result = dutyService.calculateTariff(tariffSchedule, 800.0);

        // Assert - 12.5% of $800 = $100
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    void testCalculateTariff_WithTariffSchedule_Specific() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithSpecificDuty(7.5, 2);

        // Act
        double result = dutyService.calculateTariff(tariffSchedule, 60.0);

        // Assert - (60 / 2) * $7.5 = $225
        assertThat(result).isEqualTo(225.0);
    }

    @Test
    void testCalculateTariff_WithTariffSchedule_Combined() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithCombinedDuty(8.0, 4.0, 1, "C");

        // Act
        double result = dutyService.calculateTariff(tariffSchedule, 200.0);

        // Assert - AdValorem: (200 * 8% = $16) + Specific: (200/1 * $4 = $800) = $816
        assertThat(result).isEqualTo(816.0);
    }

    @Test
    void testCalculateTariff_WithTariffSchedule_NullTariffSchedule() {
        // Act & Assert
        TariffNotFoundException exception = assertThrows(TariffNotFoundException.class, () -> {
            dutyService.calculateTariff(null, 100.0);
        });

        assertThat(exception.getMessage()).contains("TariffSchedule is null");
    }

    @Test
    void testCalculateTariff_WithTariffSchedule_NoDuty() {
        // Arrange
        TariffSchedule tariffSchedule = createBasicTariffSchedule("USA", "MEX", "020304");
        tariffSchedule.setDuty(null);
        tariffSchedule.setTariffId(30001);

        // Act & Assert
        DutyNotFoundException exception = assertThrows(DutyNotFoundException.class, () -> {
            dutyService.calculateTariff(tariffSchedule, 500.0);
        });

        assertThat(exception.getMessage()).contains("No duty information found for TariffSchedule ID: 30001");
    }

    @Test
    void testCalculateTariff_WithTariffSchedule_InvalidAmount() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(10.0);

        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            dutyService.calculateTariff(tariffSchedule, -50.0);
        });

        assertThat(exception.getMessage()).contains("Amount of product must be greater than 0");
    }

    @Test
    void testCalculateTariff_WithTariffSchedule_ZeroAmount() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(5.0);

        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            dutyService.calculateTariff(tariffSchedule, 0.0);
        });

        assertThat(exception.getMessage()).contains("Amount of product must be greater than 0");
    }

    // =================== ADDITIONAL EDGE CASE TESTS ===================

    @Test
    void testCalculateTariff_OtherDutyType_ReturnsZero() {
        // Arrange
        Country reporter = TestEntityFactory.createReporterCountry();
        Country partner = TestEntityFactory.createPartnerCountry();
        Product product = TestEntityFactory.createProduct();
        
        // Create an OtherDuty (not AdValorem, Specific, or Combined)
        OtherDuty otherDuty = TestEntityFactory.createOtherDuty();
        
        TariffSchedule tariffSchedule = TestEntityFactory.createTariffSchedule(
            reporter, partner, product, TestEntityFactory.createDutyType(), otherDuty);

        // Act
        double result = dutyService.calculateTariff(tariffSchedule, 1000.0);

        // Assert - OtherDuty should return 0.0
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void testCalculateTariff_VeryLargeAmount() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(5.0);

        // Act
        double result = dutyService.calculateTariff(tariffSchedule, 1000000.0);

        // Assert - 5% of $1,000,000 = $50,000
        assertThat(result).isEqualTo(50000.0);
    }

    @Test
    void testCalculateTariff_VerySmallAmount() {
        // Arrange
        TariffSchedule tariffSchedule = createTariffWithAdValoremDuty(3.5);

        // Act
        double result = dutyService.calculateTariff(tariffSchedule, 0.01);

        // Assert - 3.5% of $0.01 = $0.000350, rounded to $0.00
        assertThat(result).isEqualTo(0.0);
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
