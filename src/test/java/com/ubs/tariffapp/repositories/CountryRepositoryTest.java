package com.ubs.tariffapp.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.testutils.TestEntityFactory;

@DataJpaTest
public class CountryRepositoryTest {

    @Autowired
    private CountryRepository countryRepository;

    @Test
    void testSaveAndFindById() {
        Country country = TestEntityFactory.createCountry();
        countryRepository.save(country);

        Country found = countryRepository.findById(country.getCountryId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getCountryName()).isEqualTo(country.getCountryName());
        assertThat(found.getIsoCode()).isEqualTo(country.getIsoCode());
        assertThat(found.getReportedTariffs()).isEqualTo(country.getReportedTariffs());
        assertThat(found.getPartneredTariffs()).isEqualTo(country.getPartneredTariffs());
    }

    @Test
    void testDelete() {
        Country country = TestEntityFactory.createCountry();
        countryRepository.save(country);

        countryRepository.deleteById(country.getCountryId());
        assertThat(countryRepository.findById(country.getCountryId())).isEmpty();
    }
}