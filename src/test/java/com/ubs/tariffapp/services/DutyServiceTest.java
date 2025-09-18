package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
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
import java.math.BigDecimal;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DutyServiceTest {

    @Autowired
    private DutyService dutyService;
    
    @MockBean
    private TariffScheduleService tariffScheduleService;

    @Test
    void testCalculateTariff_AdValoremDuty() {
        // Creating test entities using setters
        Country reporter = new Country();
        reporter.setCountryId("USA");
        reporter.setCountryName("United States");
        reporter.setIsoCode("US");
        
        Country partner = new Country();
        partner.setCountryId("CHN");
        partner.setCountryName("China");
        partner.setIsoCode("CN");
        
        Product product = new Product();
        product.setTlCode("010121");
        product.setDescription("Live horses");
        product.setDigits(6);
        
        TariffSchedule tariffSchedule = new TariffSchedule();
        tariffSchedule.setReporter(reporter);
        tariffSchedule.setPartner(partner);
        tariffSchedule.setProduct(product);
        
        // Create AdValoremDuty with 10% rate
        AdValoremDuty adValoremDuty = new AdValoremDuty();
        adValoremDuty.setRatePercent(BigDecimal.valueOf(10.0));
        adValoremDuty.setTariffSchedule(tariffSchedule);
        
        tariffSchedule.setDuty(adValoremDuty);
        
        // Mock the service call
        when(tariffScheduleService.getTariffSchedule("USA", "CHN", "010121"))
            .thenReturn(tariffSchedule);

        // Act - Calculate tariff for $1000 worth of goods
        double result = dutyService.calculateTariff("USA", "CHN", "010121", 1000.0);

        // Assert - 10% of $1000 = $100
        assertThat(result).isEqualTo(100.0);
    }

    @Test
    void testCalculateTariff_SpecificDuty() {
        // Arrange
        Country reporter = new Country();
        reporter.setCountryId("USA");
        reporter.setCountryName("United States");
        reporter.setIsoCode("US");
        
        Country partner = new Country();
        partner.setCountryId("DEU");
        partner.setCountryName("Germany");
        partner.setIsoCode("DE");
        
        Product product = new Product();
        product.setTlCode("010129");
        product.setDescription("Live horses, other");
        product.setDigits(6);
        
        TariffSchedule tariffSchedule = new TariffSchedule();
        tariffSchedule.setReporter(reporter);
        tariffSchedule.setPartner(partner);
        tariffSchedule.setProduct(product);
        
        // Create SpecificDuty: $5 per unit, multiplier 1
        SpecificDuty specificDuty = new SpecificDuty();
        specificDuty.setAmount(BigDecimal.valueOf(5.0));
        specificDuty.setMultiplier(1);
        specificDuty.setTariffSchedule(tariffSchedule);
        
        tariffSchedule.setDuty(specificDuty);
        
        when(tariffScheduleService.getTariffSchedule("USA", "DEU", "010129"))
            .thenReturn(tariffSchedule);

        // Act - Calculate for 100 units
        double result = dutyService.calculateTariff("USA", "DEU", "010129", 100.0);

        // Assert - (100 / 1) * $5 = $500
        assertThat(result).isEqualTo(500.0);
    }

    @Test
    void testCalculateTariff_CombinedDuty_Mixed() {
        // Arrange
        Country reporter = new Country();
        reporter.setCountryId("USA");
        reporter.setCountryName("United States");
        reporter.setIsoCode("US");
        
        Country partner = new Country();
        partner.setCountryId("FRA");
        partner.setCountryName("France");
        partner.setIsoCode("FR");
        
        Product product = new Product();
        product.setTlCode("010130");
        product.setDescription("Live asses");
        product.setDigits(6);
        
        TariffSchedule tariffSchedule = new TariffSchedule();
        tariffSchedule.setReporter(reporter);
        tariffSchedule.setPartner(partner);
        tariffSchedule.setProduct(product);
        
        // Create CombinedDuty: 5% + $2 per unit (Mixed)
        CombinedDuty combinedDuty = new CombinedDuty();
        combinedDuty.setRatePercent(BigDecimal.valueOf(5.0));
        combinedDuty.setAmount(BigDecimal.valueOf(2.0));
        combinedDuty.setMultiplier(1);
        combinedDuty.setMixedOrConditional("M"); // Fixed: Use String instead of char
        combinedDuty.setTariffSchedule(tariffSchedule);
        
        tariffSchedule.setDuty(combinedDuty);
        
        when(tariffScheduleService.getTariffSchedule("USA", "FRA", "010130"))
            .thenReturn(tariffSchedule);

        // Act - Calculate for $1000 worth, 50 units
        double result = dutyService.calculateTariff("USA", "FRA", "010130", 50.0);

        // Assert - AdValorem: (50 * 5%) + Specific: (50/1 * $2) = $2.5 + $100 = $102.5
        assertThat(result).isEqualTo(102.5);
    }

    @Test
    void testCalculateTariff_CombinedDuty_Conditional() {
        // Arrange
        Country reporter = new Country();
        reporter.setCountryId("USA");
        reporter.setCountryName("United States");
        reporter.setIsoCode("US");
        
        Country partner = new Country();
        partner.setCountryId("GBR");
        partner.setCountryName("United Kingdom");
        partner.setIsoCode("GB");
        
        Product product = new Product();
        product.setTlCode("010190");
        product.setDescription("Live horses, other");
        product.setDigits(6);
        
        TariffSchedule tariffSchedule = new TariffSchedule();
        tariffSchedule.setReporter(reporter);
        tariffSchedule.setPartner(partner);
        tariffSchedule.setProduct(product);
        
        // Create CombinedDuty: 2% or $10 per unit, whichever is higher (Conditional)
        CombinedDuty combinedDuty = new CombinedDuty();
        combinedDuty.setRatePercent(BigDecimal.valueOf(2.0));
        combinedDuty.setAmount(BigDecimal.valueOf(10.0));
        combinedDuty.setMultiplier(1);
        combinedDuty.setMixedOrConditional("C"); // Fixed: Use String instead of char
        combinedDuty.setTariffSchedule(tariffSchedule);
        
        tariffSchedule.setDuty(combinedDuty);
        
        when(tariffScheduleService.getTariffSchedule("USA", "GBR", "010190"))
            .thenReturn(tariffSchedule);

        // Act - Calculate for 20 units
        double result = dutyService.calculateTariff("USA", "GBR", "010190", 20.0);

        // Assert - AdValorem: (20 * 2% = $0.4), Specific: (20/1 * $10 = $200)
        // Max($0.4, $200) = $200
        assertThat(result).isEqualTo(200.0);
    }

    @Test
    void testCalculateTariff_RoundingBehavior() {
        // Arrange
        Country reporter = new Country();
        reporter.setCountryId("USA");
        reporter.setCountryName("United States");
        reporter.setIsoCode("US");
        
        Country partner = new Country();
        partner.setCountryId("JPN");
        partner.setCountryName("Japan");
        partner.setIsoCode("JP");
        
        Product product = new Product();
        product.setTlCode("010200");
        product.setDescription("Live bovine");
        product.setDigits(6);
        
        TariffSchedule tariffSchedule = new TariffSchedule();
        tariffSchedule.setReporter(reporter);
        tariffSchedule.setPartner(partner);
        tariffSchedule.setProduct(product);
        
        // Create AdValoremDuty with rate that creates rounding scenario
        AdValoremDuty adValoremDuty = new AdValoremDuty();
        adValoremDuty.setRatePercent(BigDecimal.valueOf(7.333)); // Creates decimal result
        adValoremDuty.setTariffSchedule(tariffSchedule);
        
        tariffSchedule.setDuty(adValoremDuty);
        
        when(tariffScheduleService.getTariffSchedule("USA", "JPN", "010200"))
            .thenReturn(tariffSchedule);

        // Act
        double result = dutyService.calculateTariff("USA", "JPN", "010200", 133.33);

        // Assert - Result should be rounded to 2 decimal places
        // 133.33 * 7.333% = 9.777... should round to 9.78
        assertThat(result).isEqualTo(9.78);
    }

    @Test
    void testCalculateTariff_NoDutyFound() {
        // Mock service to return null
        when(tariffScheduleService.getTariffSchedule("XXX", "YYY", "NOTFOUND"))
            .thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dutyService.calculateTariff("XXX", "YYY", "NOTFOUND", 1000.0);
        });

        assertThat(exception.getMessage()).contains("No tariff schedule found");
    }

    @Test
    void testCalculateTariff_ServiceNotNull() {
        assertThat(dutyService).isNotNull();
    }
}
