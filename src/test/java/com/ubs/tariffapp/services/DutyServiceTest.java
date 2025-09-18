package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ubs.tariffapp.Services.DutyService;
import com.ubs.tariffapp.repositories.DutyRepository;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.models.duty.OtherDuty;
import com.ubs.tariffapp.testutils.TestEntityFactory;
import java.math.BigDecimal;

@SpringBootTest
@ActiveProfiles("test") // loads application-test.properties
@Transactional // rollback after each test
public class DutyServiceTest {

    @Autowired
    private DutyService dutyService;
    
    @Autowired
    private DutyRepository dutyRepository;

    @Test
    void testCalculateTariff_AdValoremDuty() {
        // Arrange
        AdValoremDuty adValoremDuty = TestEntityFactory.createAdValoremDuty();
        adValoremDuty.setRatePercent(BigDecimal.valueOf(10.0)); // 10%
        adValoremDuty.setImporterCountry("US");
        adValoremDuty.setExporterCountry("CN");
        adValoremDuty.setProductCode("TEST001");
        dutyRepository.save(adValoremDuty);

        // Act
        double result = dutyService.calculateTariff("US", "CN", "TEST001", 1000.0);

        // Assert
        assertThat(result).isEqualTo(100.0); // 1000 * 10% = 100
    }

    @Test
    void testCalculateTariff_SpecificDuty() {
        // Arrange
        SpecificDuty specificDuty = TestEntityFactory.createSpecificDuty();
        specificDuty.setAmount(BigDecimal.valueOf(50.0));
        specificDuty.setMultiplier(BigDecimal.valueOf(10.0));
        specificDuty.setImporterCountry("US");
        specificDuty.setExporterCountry("CN");
        specificDuty.setProductCode("TEST002");
        dutyRepository.save(specificDuty);

        // Act
        double result = dutyService.calculateTariff("US", "CN", "TEST002", 100.0);

        // Assert
        assertThat(result).isEqualTo(500.0); // (100/10) * 50 = 500
    }

    @Test
    void testCalculateTariff_CombinedDuty_Mixed() {
        // Arrange
        CombinedDuty combinedDuty = TestEntityFactory.createCombinedDuty();
        combinedDuty.setRatePercent(BigDecimal.valueOf(5.0)); // 5%
        combinedDuty.setAmount(BigDecimal.valueOf(20.0));
        combinedDuty.setMultiplier(BigDecimal.valueOf(5.0));
        combinedDuty.setMixedOrConditional("M"); // Mixed
        combinedDuty.setImporterCountry("US");
        combinedDuty.setExporterCountry("CN");
        combinedDuty.setProductCode("TEST003");
        dutyRepository.save(combinedDuty);

        // Act
        double result = dutyService.calculateTariff("US", "CN", "TEST003", 1000.0);

        // Assert
        // Ad valorem: 1000 * 5% = 50
        // Specific: (1000/5) * 20 = 4000
        // Mixed: 50 + 4000 = 4050
        assertThat(result).isEqualTo(4050.0);
    }

    @Test
    void testCalculateTariff_CombinedDuty_Conditional() {
        // Arrange
        CombinedDuty combinedDuty = TestEntityFactory.createCombinedDuty();
        combinedDuty.setRatePercent(BigDecimal.valueOf(15.0)); // 15%
        combinedDuty.setAmount(BigDecimal.valueOf(10.0));
        combinedDuty.setMultiplier(BigDecimal.valueOf(10.0));
        combinedDuty.setMixedOrConditional("C"); // Conditional
        combinedDuty.setImporterCountry("US");
        combinedDuty.setExporterCountry("CN");
        combinedDuty.setProductCode("TEST004");
        dutyRepository.save(combinedDuty);

        // Act
        double result = dutyService.calculateTariff("US", "CN", "TEST004", 1000.0);

        // Assert
        // Ad valorem: 1000 * 15% = 150
        // Specific: (1000/10) * 10 = 1000
        // Conditional: max(150, 1000) = 1000
        assertThat(result).isEqualTo(1000.0);
    }

    @Test
    void testCalculateTariff_NoDutyFound() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dutyService.calculateTariff("XX", "YY", "NOTFOUND", 1000.0);
        });

        assertThat(exception.getMessage()).isEqualTo("No duty found for the given countries and product.");
    }

    @Test
    void testCalculateTariff_RoundingTest() {
        // Arrange
        AdValoremDuty adValoremDuty = TestEntityFactory.createAdValoremDuty();
        adValoremDuty.setRatePercent(BigDecimal.valueOf(7.333)); // Should round
        adValoremDuty.setImporterCountry("US");
        adValoremDuty.setExporterCountry("CN");
        adValoremDuty.setProductCode("ROUND001");
        dutyRepository.save(adValoremDuty);

        // Act
        double result = dutyService.calculateTariff("US", "CN", "ROUND001", 100.0);

        // Assert
        assertThat(result).isEqualTo(7.33); // 100 * 7.333% = 7.333, rounded to 7.33
    }
}
