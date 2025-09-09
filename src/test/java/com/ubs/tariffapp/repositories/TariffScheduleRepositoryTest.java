package com.ubs.tariffapp.repositories;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.ubs.tariffapp.models.AdValoremDuty;
import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.DutyTypeId;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.testutils.TestConstants;

@DataJpaTest
public class TariffScheduleRepositoryTest {
    // Testing data is from a real dataset entry
    private static final Integer TARIFF_ID = TestConstants.TARIFF_ID;
    private static final Integer TARIFF_YEAR = TestConstants.TARIFF_YEAR;
    private static final String TLS_SUFFIX = TestConstants.TLS_SUFFIX;
    private static final String NOTE = TestConstants.NOTE;

    private static final String REPORTER_ID = TestConstants.REPORTER_ID;
    private static final String REPORTER_NAME = TestConstants.REPORTER_NAME;
    private static final String REPORTER_ISO = TestConstants.REPORTER_ISO;

    private static final String PARTNER_ID = TestConstants.PARTNER_ID;
    private static final String PARTNER_NAME = TestConstants.PARTNER_NAME;
    private static final String PARTNER_ISO = TestConstants.PARTNER_ISO;

    private static final String PRODUCT_CODE = TestConstants.PRODUCT_TL_CODE;
    private static final String PRODUCT_DESC = TestConstants.PRODUCT_DESCRIPTION;
    private static final Integer PRODUCT_DIGITS = PRODUCT_CODE.length();

    private static final String DUTY_TYPE = TestConstants.DUTY_TYPE;
    private static final String DUTY_CODE = TestConstants.DUTY_CODE;
    private static final String DUTY_DESC = TestConstants.DUTY_TYPE_DESCRIPTION;

    // For AdValoremDuty
    // TODO: Move to TestConstants if needed in other tests
    private static final String MATH_EXPRESSION = "0";
    private static final BigDecimal RATE_PERCENT = BigDecimal.ZERO;

    @Autowired
    private TariffScheduleRepository repository;

    // Repositories for related entities (to persist them first)
    @Autowired
    private DutyTypeRepository dutyTypeRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testSaveAndFindById() {
        // Parent Java objects do not have their lists updated in memory when children
        // are added
        // But the database relationships are correctly established
        Country reporter = new Country(REPORTER_ID, REPORTER_NAME, REPORTER_ISO, Collections.emptyList(),
                Collections.emptyList());
        Country partner = new Country(PARTNER_ID, PARTNER_NAME, PARTNER_ISO, Collections.emptyList(),
                Collections.emptyList());
        Product product = new Product(PRODUCT_CODE, PRODUCT_DESC, PRODUCT_DIGITS, Collections.emptyList());
        DutyTypeId dutyTypeId = new DutyTypeId(DUTY_TYPE, DUTY_CODE);
        DutyType dutyType = new DutyType(dutyTypeId, DUTY_DESC, Collections.emptyList());

        // Persist related entities first
        countryRepository.save(reporter);
        countryRepository.save(partner);
        productRepository.save(product);
        dutyTypeRepository.save(dutyType);

        // No need to persist Duty, it will be cascaded (see TariffSchedule model)

        AdValoremDuty duty = new AdValoremDuty(
                TARIFF_ID,
                null, // TariffSchedule will be set later
                DUTY_DESC,
                MATH_EXPRESSION,
                RATE_PERCENT);

        TariffSchedule schedule = new TariffSchedule(
                TARIFF_ID,
                reporter,
                partner,
                TARIFF_YEAR,
                product,
                TLS_SUFFIX,
                dutyType,
                NOTE,
                duty,
                Collections.emptyList() // auditLogs
        );

        duty.setTariffSchedule(schedule);
        repository.save(schedule);

        Integer generatedId = schedule.getTariffId();

        TariffSchedule found = repository.findById(generatedId).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getTariffYear()).isEqualTo(TARIFF_YEAR);
        assertThat(found.getTlsSuffix()).isEqualTo(TLS_SUFFIX);
        assertThat(found.getNote()).isEqualTo(NOTE);
        assertThat(found.getReporter().getCountryName()).isEqualTo(REPORTER_NAME);
        assertThat(found.getPartner().getCountryName()).isEqualTo(PARTNER_NAME);
        assertThat(found.getProduct().getDescription()).isEqualTo(PRODUCT_DESC);
        assertThat(found.getDutyType().getDutyTypeDescription()).isEqualTo(DUTY_DESC);
        assertThat(found.getDuty()).isNotNull(); // Check for cascade persistnce for Duty entity
        assertThat(found.getDuty().getMathExpression()).isEqualTo(MATH_EXPRESSION);
        assertThat(((AdValoremDuty) found.getDuty()).getRatePercent()).isEqualByComparingTo(RATE_PERCENT);
    }

    @Test
    void testDelete() {
        Country reporter = new Country(REPORTER_ID, REPORTER_NAME, REPORTER_ISO, Collections.emptyList(),
                Collections.emptyList());
        Country partner = new Country(PARTNER_ID, PARTNER_NAME, PARTNER_ISO, Collections.emptyList(),
                Collections.emptyList());
        Product product = new Product(PRODUCT_CODE, PRODUCT_DESC, PRODUCT_DIGITS, Collections.emptyList());
        DutyTypeId dutyTypeId = new DutyTypeId(DUTY_TYPE, DUTY_CODE);
        DutyType dutyType = new DutyType(dutyTypeId, DUTY_DESC, Collections.emptyList());

        // Persist related entities first
        countryRepository.save(reporter);
        countryRepository.save(partner);
        productRepository.save(product);
        dutyTypeRepository.save(dutyType);

        AdValoremDuty duty = new AdValoremDuty(
                TARIFF_ID,
                null, // TariffSchedule will be set later
                DUTY_DESC,
                MATH_EXPRESSION,
                RATE_PERCENT);

        TariffSchedule schedule = new TariffSchedule(
                TARIFF_ID,
                reporter,
                partner,
                TARIFF_YEAR,
                product,
                TLS_SUFFIX,
                dutyType,
                NOTE,
                duty,
                Collections.emptyList() // auditLogs
        );

        duty.setTariffSchedule(schedule);
        repository.save(schedule);

        Integer generatedId = schedule.getTariffId();

        repository.deleteById(generatedId);
        assertThat(repository.findById(generatedId)).isEmpty();
    }
}
