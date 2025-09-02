package com.ubs.tariffapp.repositories;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.ubs.tariffapp.models.Surcharge;
import com.ubs.tariffapp.models.Tariff;

// Use "mvn test" or the Test Runner extension to run test
// No output should show if the test passes
// Data also doesn't persist btw

// TODO: Add more robust tests

@DataJpaTest
public class TariffRepositoryTest {
    private static final String ORIGIN = "ftaCode#hsCode";
    private static final double TAX_RATE = 0.05;
    private static final String TYPE = "environmental";
    private static final double SURCHARGE_AMOUNT = 10.0;
    private static final String CURRENCY = "SGD";

    @Autowired
    private TariffRepository tariffRepository;

    @Test
    void testSaveAndFindTariff() {
        Surcharge surcharge = new Surcharge(TYPE, SURCHARGE_AMOUNT, CURRENCY);
        Tariff tariff = new Tariff(
            ORIGIN,
            LocalDateTime.now(),
            TAX_RATE,
            Collections.singletonList(surcharge),
            CURRENCY,
            LocalDateTime.now(),
            "Test tariff"
        );

        tariffRepository.save(tariff);

        Tariff found = tariffRepository.findById(ORIGIN).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getBaseCurrency()).isEqualTo(CURRENCY);
        assertThat(found.getSurcharges()).hasSize(1); // We only added one surcharge
    }
}