package com.ubs.tariffapp.repositories;

import com.ubs.tariffapp.models.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

@DataJpaTest
public class CountryRepositoryTest {
    private static final String COUNTRY_ID = "004";
    private static final String COUNTRY_NAME = "Afghanistan";
    private static final String ISO_CODE = "AFG";

    @Autowired
    private CountryRepository countryRepository;

    @Test
    void testSaveAndFindCountry() {
        Country country = new Country(COUNTRY_ID, COUNTRY_NAME, ISO_CODE,
                Collections.emptyList(), Collections.emptyList());
        countryRepository.save(country);

        Country found = countryRepository.findById(COUNTRY_ID).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCountryName()).isEqualTo(COUNTRY_NAME);
        assertThat(found.getIsoCode()).isEqualTo(ISO_CODE);
        assertThat(found.getReportedTariffs()).isEmpty();
        assertThat(found.getPartneredTariffs()).isEmpty();
    }
}