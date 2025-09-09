package com.ubs.tariffapp.repositories;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.testutils.TestConstants;

@DataJpaTest
public class CountryRepositoryTest {
    private static final String COUNTRY_ID = TestConstants.COUNTRY_ID;
    private static final String COUNTRY_NAME = TestConstants.COUNTRY_NAME;
    private static final String ISO_CODE = TestConstants.COUNTRY_ISO_CODE;
    private static final List<TariffSchedule> REPORTED_TARIFFS = TestConstants.COUNTRY_REPORTED_TARIFFS;
    private static final List<TariffSchedule> PARTNERED_TARIFFS = TestConstants.COUNTRY_PARTNERED_TARIFFS;

    @Autowired
    private CountryRepository countryRepository;

    @Test
    void testSaveAndFindById() {
        Country country = new Country(COUNTRY_ID, COUNTRY_NAME, ISO_CODE,
                REPORTED_TARIFFS, PARTNERED_TARIFFS);
        countryRepository.save(country);

        Country found = countryRepository.findById(COUNTRY_ID).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCountryName()).isEqualTo(COUNTRY_NAME);
        assertThat(found.getIsoCode()).isEqualTo(ISO_CODE);
        assertThat(found.getReportedTariffs()).isEmpty();
        assertThat(found.getPartneredTariffs()).isEmpty();
    }

    @Test
    void testDelete() {
        Country country = new Country(COUNTRY_ID, COUNTRY_NAME, ISO_CODE,
                REPORTED_TARIFFS, PARTNERED_TARIFFS);
        countryRepository.save(country);
        countryRepository.deleteById(COUNTRY_ID);
        assertThat(countryRepository.findById(COUNTRY_ID)).isEmpty();
    }
}