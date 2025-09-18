package com.ubs.tariffapp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.ubs.tariffapp.services.DutyService;
import com.ubs.tariffapp.services.TariffScheduleService;
import com.ubs.tariffapp.repositories.duty.AdValoremDutyRepository;
import com.ubs.tariffapp.repositories.duty.SpecificDutyRepository;
import com.ubs.tariffapp.repositories.duty.CombinedDutyRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.ProductRepository;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.testutils.TestEntityFactory;
import java.math.BigDecimal;

@SpringBootTest
@ActiveProfiles("test") // loads application-test.properties
@Transactional // rollback after each test
public class DutyServiceTest {

    @Autowired
    private DutyService dutyService;
    
    @Autowired
    private TariffScheduleService tariffScheduleService;
    
    @Autowired
    private TariffScheduleRepository tariffScheduleRepository;
    
    @Autowired
    private AdValoremDutyRepository adValoremDutyRepository;
    
    @Autowired
    private CountryRepository countryRepository;
    
    @Autowired
    private ProductRepository productRepository;

    @Test
    void testCalculateTariff_AdValoremDuty() {
        // Arrange - Create countries and product
        Country reporter = new Country();
        reporter.setCountryId("US");
        reporter.setCountryName("United States");
        countryRepository.save(reporter);
        
        Country partner = new Country();
        partner.setCountryId("CN");
        partner.setCountryName("China");
        countryRepository.save(partner);
        
        Product product = new Product();
        product.setTlCode("TEST001");
        product.setDescription("Test Product");
        productRepository.save(product);
        
        // Create AdValoremDuty
        AdValoremDuty duty = new AdValoremDuty();
        duty.setRatePercent(BigDecimal.valueOf(10.0));
        adValoremDutyRepository.save(duty);
        
        // Create TariffSchedule
        TariffSchedule tariffSchedule = new TariffSchedule();
        tariffSchedule.setReporter(reporter);
        tariffSchedule.setPartner(partner);
        tariffSchedule.setProduct(product);
        tariffSchedule.setDuty(duty);
        tariffScheduleRepository.save(tariffSchedule);

        // Act
        double result = dutyService.calculateTariff("US", "CN", "TEST001", 1000.0);

        // Assert
        assertThat(result).isEqualTo(100.0); // 1000 * 10% = 100
    }

    @Test
    void testCalculateTariff_NoDutyFound() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            dutyService.calculateTariff("XX", "YY", "NOTFOUND", 1000.0);
        });

        assertThat(exception.getMessage()).contains("TariffSchedule not found");
    }
}
