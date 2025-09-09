package com.ubs.tariffapp.testutils;

import java.util.Collections;
import java.util.List;

import com.ubs.tariffapp.models.TariffSchedule;

public class TestConstants {
    // For CountryRepositoryTest
    public static final String COUNTRY_ID = "004";
    public static final String COUNTRY_NAME = "Afghanistan";
    public static final String COUNTRY_ISO_CODE = "AFG";
    public static final List<TariffSchedule> COUNTRY_REPORTED_TARIFFS = Collections.emptyList();
    public static final List<TariffSchedule> COUNTRY_PARTNERED_TARIFFS = Collections.emptyList();

    // For ProductRepositoryTest
    public static final String PRODUCT_TL_CODE = "72299005";
    public static final String PRODUCT_DESCRIPTION = "Alloy high-speed steel, wire";
    public static final List<TariffSchedule> PRODUCT_TARIFF_SCHEDULES = Collections.emptyList();

    // For DutyTypeRepositoryTest
    public static final String DUTY_TYPE = "1";
	public static final String DUTY_CODE = "A";
	public static final String DUTY_TYPE_DESCRIPTION = "Free Trade Area duty rate for Dominican Rep. and Central America (DR-CAFTA)";
    public static final List<TariffSchedule> DUTY_TYPE_TARIFF_SCHEDULES = Collections.emptyList();
}
