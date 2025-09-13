package com.ubs.tariffapp.testutils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ubs.tariffapp.models.AuditLog;
import com.ubs.tariffapp.models.TariffSchedule;

public class TestConstants {
    /*
     * Constants for testing purposes (taken from actual data)
     * However, not all fields accurately reflect real-world data
     * E.g. between USA and World, for the product with TL code 19019061 in 2023,
     * it is an ad valorem duty with a rate of 0.0%, but we use this data
     * to test both AdValoremDuty and SpecificDuty entities, using hypothetical values
     * for SpecificDuty taken from another product
     */
    // For CountryRepositoryTest
    public static final String COUNTRY_ID = "004";
    public static final String COUNTRY_NAME = "Afghanistan";
    public static final String COUNTRY_ISO_CODE = "AFG";
    public static final List<TariffSchedule> COUNTRY_REPORTED_TARIFFS = new ArrayList<>();
    public static final List<TariffSchedule> COUNTRY_PARTNERED_TARIFFS = new ArrayList<>();

    // For ProductRepositoryTest
    public static final String PRODUCT_TL_CODE = "19019061";
    public static final String PRODUCT_DESCRIPTION = "Malted milk described in additional US note 10 to chapter 4: provisional";
    public static final List<TariffSchedule> PRODUCT_TARIFF_SCHEDULES = new ArrayList<>();

    // For DutyTypeRepositoryTest
    public static final String DUTY_TYPE = "1";
    public static final String DUTY_CODE = "1";
    public static final String DUTY_TYPE_DESCRIPTION = "Free-trade area duty rates for Mexico under the NAFTA";
    public static final List<TariffSchedule> DUTY_TYPE_TARIFF_SCHEDULES = new ArrayList<>();

    // For TariffScheduleRepositoryTest
    public static final Integer TARIFF_ID = null; // Changed to null to allow auto-generation
    public static final Integer TARIFF_YEAR = 2023;
    public static final String TLS_SUFFIX = "";
    public static final String NOTE = "TL_IN added";

    public static final String REPORTER_ID = "840";
    public static final String REPORTER_NAME = "United States";
    public static final String REPORTER_ISO = "USA";
    public static final List<TariffSchedule> REPORTER_REPORTED_TARIFFS = new ArrayList<>();
    public static final List<TariffSchedule> REPORTER_PARTNERED_TARIFFS = new ArrayList<>();

    public static final String PARTNER_ID = "000";
    public static final String PARTNER_NAME = "World";
    public static final String PARTNER_ISO = "WLD";
    public static final List<TariffSchedule> PARTNER_REPORTED_TARIFFS = new ArrayList<>();
    public static final List<TariffSchedule> PARTNER_PARTNERED_TARIFFS = new ArrayList<>();

    public static final List<AuditLog> AUDIT_LOGS = new ArrayList<>();

    // For generic Duty fields (for a none duty where dutyNature = "N")
    public static final String NONE_DUTY_NATURE = "N"; // For NoneDuty
    public static final String NONE_MATH_EXPRESSION = ""; // No duty

    
    // For AdValoremDuty fields
    public static final String AD_VALOREM_DUTY_NATURE = "A"; // For AdValoremDuty
    public static final String AD_VALOREM_MATH_EXPRESSION = "0 * value";
    public static final BigDecimal AD_VALOREM_RATE_PERCENT = BigDecimal.ZERO;

    // For SpecificDuty fields
    public static final String SPECIFIC_DUTY_NATURE = "S"; // For SpecificDuty
    // specific duty rate of 0.34 cents/liter
    public static final String SPECIFIC_MATH_EXPRESSION = "0.0034 * num_units"; 
    public static final BigDecimal SPECIFIC_AMOUNT = BigDecimal.valueOf(0.34); 
    public static final String SPECIFIC_UNIT = "liter";
    public static final Integer SPECIFIC_MULTIPLIER = 1; // Since rate is per 1 liter
    public static final String SPECIFIC_DUTY_RATE_RAW = "0.34 cents/liter";
    // TODO: Find a way to parse raw representation into structured fields

    // Note: We reuse the fields for CombinedDuty from both AdValoremDuty and SpecificDuty
    public static final String COMBINED_DUTY_NATURE = "C"; // For CombinedDuty
    public static final String MIXED_OR_CONDITIONAL = "M"; // 'M'ixed or 'C'onditional
    public static final String COMBINED_MATH_EXPRESSION = SPECIFIC_MATH_EXPRESSION + " + " + AD_VALOREM_MATH_EXPRESSION;

    // For OtherDuty fields
    public static final String OTHER_DUTY_NATURE = "O"; // For OtherDuty
    public static final String OTHER_MATH_EXPRESSION = "";
    public static final String OTHER_RAW_TEXT = "See 9822.04.01-9822.04.03";
    public static final Boolean OTHER_IS_COMPUTABLE = false;

    // For AuditLogRepositoryTest
    public static final Integer AUDIT_LOG_ID = null; // Changed to null to allow auto-generation
    public static final String AUDIT_CHANGE_TYPE = "INSERT";
    public static final String AUDIT_CHANGED_BY = "test_admin";
    public static final LocalDateTime AUDIT_CHANGE_DATE = LocalDateTime.of(2023, 1, 1, 12, 0);
    public static final String AUDIT_CHANGE_DETAILS = "Created tariff schedule entry for testing";

}
