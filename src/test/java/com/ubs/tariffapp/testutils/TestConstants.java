package com.ubs.tariffapp.testutils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.ubs.tariffapp.models.AuditLog;
import com.ubs.tariffapp.models.TariffSchedule;

public class TestConstants {
    // For CountryRepositoryTest
    public static final String COUNTRY_ID = "004";
    public static final String COUNTRY_NAME = "Afghanistan";
    public static final String COUNTRY_ISO_CODE = "AFG";
    public static final List<TariffSchedule> COUNTRY_REPORTED_TARIFFS = Collections.emptyList();
    public static final List<TariffSchedule> COUNTRY_PARTNERED_TARIFFS = Collections.emptyList();

    // For ProductRepositoryTest
    public static final String PRODUCT_TL_CODE = "19019061";
    public static final String PRODUCT_DESCRIPTION = "Malted milk described in additional US note 10 to chapter 4: provisional";
    public static final List<TariffSchedule> PRODUCT_TARIFF_SCHEDULES = Collections.emptyList();

    // For DutyTypeRepositoryTest
    public static final String DUTY_TYPE = "1";
    public static final String DUTY_CODE = "1";
    public static final String DUTY_TYPE_DESCRIPTION = "Free-trade area duty rates for Mexico under the NAFTA";
    public static final List<TariffSchedule> DUTY_TYPE_TARIFF_SCHEDULES = Collections.emptyList();

    // For TariffScheduleRepositoryTest
    public static final Integer TARIFF_ID = null; // Changed to null to allow auto-generation
    public static final Integer TARIFF_YEAR = 2023;
    public static final String TLS_SUFFIX = "";
    public static final String NOTE = "TL_IN added";

    public static final String REPORTER_ID = "840";
    public static final String REPORTER_NAME = "United States";
    public static final String REPORTER_ISO = "USA";
    public static final List<TariffSchedule> REPORTER_REPORTED_TARIFFS = Collections.emptyList();
    public static final List<TariffSchedule> REPORTER_PARTNERED_TARIFFS = Collections.emptyList();

    public static final String PARTNER_ID = "000";
    public static final String PARTNER_NAME = "World";
    public static final String PARTNER_ISO = "WLD";
    public static final List<TariffSchedule> PARTNER_REPORTED_TARIFFS = Collections.emptyList();
    public static final List<TariffSchedule> PARTNER_PARTNERED_TARIFFS = Collections.emptyList();

    public static final List<AuditLog> AUDIT_LOGS = Collections.emptyList();

    // For AdValoremDuty fields
    public static final String MATH_EXPRESSION = "0";
    public static final BigDecimal RATE_PERCENT = BigDecimal.ZERO;

    // For SpecificDuty fields
    


}
