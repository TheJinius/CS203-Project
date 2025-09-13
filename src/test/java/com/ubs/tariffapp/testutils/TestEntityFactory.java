package com.ubs.tariffapp.testutils;

import java.util.ArrayList;

import com.ubs.tariffapp.models.AuditLog;
import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.DutyTypeId;
import com.ubs.tariffapp.models.Product;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.duty.AdValoremDuty;
import com.ubs.tariffapp.models.duty.CombinedDuty;
import com.ubs.tariffapp.models.duty.Duty;
import com.ubs.tariffapp.models.duty.OtherDuty;
import com.ubs.tariffapp.models.duty.SpecificDuty;
import com.ubs.tariffapp.repositories.CountryRepository;
import com.ubs.tariffapp.repositories.DutyTypeRepository;
import com.ubs.tariffapp.repositories.ProductRepository;
import com.ubs.tariffapp.repositories.TariffScheduleRepository;

public class TestEntityFactory {
        // For CountryRepositoryTest
        public static Country createCountry() {
                Country country = new Country(
                                TestConstants.COUNTRY_ID,
                                TestConstants.COUNTRY_NAME,
                                TestConstants.COUNTRY_ISO_CODE,
                                new ArrayList<>(),
                                new ArrayList<>());

                return country;
        }

        // For ProductRepositoryTest
        public static Product createProduct() {
                Product product = new Product(
                                TestConstants.PRODUCT_TL_CODE,
                                TestConstants.PRODUCT_DESCRIPTION,
                                TestConstants.PRODUCT_TL_CODE.length(),
                                new ArrayList<>());

                return product;
        }

        // For TariffScheduleRepositoryTest
        public static Country createReporterCountry() {
                Country reporter = new Country(
                                TestConstants.REPORTER_ID,
                                TestConstants.REPORTER_NAME,
                                TestConstants.REPORTER_ISO,
                                new ArrayList<>(),
                                new ArrayList<>());

                return reporter;
        }

        public static Country createPartnerCountry() {
                Country partner = new Country(
                                TestConstants.PARTNER_ID,
                                TestConstants.PARTNER_NAME,
                                TestConstants.PARTNER_ISO,
                                new ArrayList<>(),
                                new ArrayList<>());

                return partner;
        }

        public static DutyType createDutyType() {
                DutyTypeId dutyTypeId = new DutyTypeId(
                                TestConstants.DUTY_TYPE,
                                TestConstants.DUTY_CODE);

                DutyType dutyType = new DutyType(
                                dutyTypeId,
                                TestConstants.DUTY_TYPE_DESCRIPTION,
                                new ArrayList<>());

                return dutyType;
        }

        public static Duty createNoneDuty() {
                Duty noneDuty = new Duty(
                                TestConstants.TARIFF_ID, // null in TestConstants - ID will be set by auto-increment
                                null, // TariffSchedule will be set later (see createTariffSchedule)
                                TestConstants.NONE_DUTY_NATURE,
                                TestConstants.NONE_MATH_EXPRESSION);
                return noneDuty;
        }

        public static AdValoremDuty createAdValoremDuty() {
                AdValoremDuty adValoremDuty = new AdValoremDuty(
                                TestConstants.TARIFF_ID, // null in TestConstants - ID will be set by auto-increment
                                null, // TariffSchedule will be set later (see createTariffSchedule)
                                TestConstants.AD_VALOREM_DUTY_NATURE,
                                TestConstants.AD_VALOREM_MATH_EXPRESSION,
                                TestConstants.AD_VALOREM_RATE_PERCENT);
                return adValoremDuty;
        }

        public static SpecificDuty createSpecificDuty() {
                SpecificDuty specificDuty = new SpecificDuty(
                                TestConstants.TARIFF_ID, // null in TestConstants - ID will be set by auto-increment
                                null, // TariffSchedule will be set later (see createTariffSchedule)
                                TestConstants.SPECIFIC_DUTY_NATURE,
                                TestConstants.SPECIFIC_MATH_EXPRESSION,
                                TestConstants.SPECIFIC_AMOUNT,
                                TestConstants.SPECIFIC_UNIT,
                                TestConstants.SPECIFIC_MULTIPLIER,
                                TestConstants.SPECIFIC_DUTY_RATE_RAW);
                return specificDuty;
        }

        public static CombinedDuty createCombinedDuty() {
                // CombinedDuty reuses fields from both AdValoremDuty and SpecificDuty
                CombinedDuty combinedDuty = new CombinedDuty(
                                TestConstants.TARIFF_ID, // null in TestConstants - ID will be set by auto-increment
                                null, // TariffSchedule will be set later (see createTariffSchedule)
                                TestConstants.COMBINED_DUTY_NATURE,
                                TestConstants.COMBINED_MATH_EXPRESSION,
                                TestConstants.MIXED_OR_CONDITIONAL,
                                TestConstants.AD_VALOREM_RATE_PERCENT,
                                TestConstants.SPECIFIC_AMOUNT,
                                TestConstants.SPECIFIC_UNIT,
                                TestConstants.SPECIFIC_MULTIPLIER,
                                TestConstants.SPECIFIC_DUTY_RATE_RAW);
                return combinedDuty;
        }

        public static OtherDuty createOtherDuty() {
                OtherDuty otherDuty = new OtherDuty(
                                TestConstants.TARIFF_ID, // null in TestConstants - ID will be set by auto-increment
                                null, // TariffSchedule will be set later (see createTariffSchedule)
                                TestConstants.OTHER_DUTY_NATURE,
                                TestConstants.OTHER_MATH_EXPRESSION,
                                TestConstants.OTHER_RAW_TEXT,
                                TestConstants.OTHER_IS_COMPUTABLE);
                return otherDuty;
        }

        public static TariffSchedule createTariffSchedule(Country reporter,
                        Country partner, Product product, DutyType dutyType, Duty duty) {

                TariffSchedule schedule = new TariffSchedule(
                                TestConstants.TARIFF_ID,
                                reporter,
                                partner,
                                TestConstants.TARIFF_YEAR,
                                product,
                                TestConstants.TLS_SUFFIX,
                                dutyType,
                                TestConstants.NOTE,
                                duty,
                                new ArrayList<>());

                duty.setTariffSchedule(schedule);
                return schedule;
        }

        /**
         * Higher level helper method to create and persist a complete TariffSchedule
         * with all its parent entities persisted as well.
         * 
         * @param countryRepo
         * @param productRepo
         * @param dutyTypeRepo
         * @param scheduleRepo
         * @param childDuty
         * @return
         */
        public static TariffSchedule createAndSaveTariffSchedule(
                        CountryRepository countryRepo,
                        ProductRepository productRepo,
                        DutyTypeRepository dutyTypeRepo,
                        TariffScheduleRepository scheduleRepo,
                        Duty childDuty) {
                // Note that parent Java objects do not have their lists updated
                // in memory when children are added
                // But the database relationships are correctly established
                Country reporter = createReporterCountry();
                Country partner = createPartnerCountry();
                Product product = createProduct();
                DutyType dutyType = createDutyType();

                countryRepo.save(reporter);
                countryRepo.save(partner);
                productRepo.save(product);
                dutyTypeRepo.save(dutyType);

                TariffSchedule schedule = createTariffSchedule(
                                reporter,
                                partner,
                                product,
                                dutyType,
                                childDuty);

                scheduleRepo.save(schedule);
                return schedule;
        }

        // // Unused variant of above method:
        // public static TariffSchedule createAndSaveTariffSchedule(
        //                 CountryRepository countryRepo,
        //                 ProductRepository productRepo,
        //                 DutyTypeRepository dutyTypeRepo,
        //                 TariffScheduleRepository scheduleRepo,
        //                 Duty childDuty) {
        //         // Note that parent Java objects do not have their lists updated
        //         // in memory when children are added
        //         // But the database relationships are correctly established
        //         Country reporter = createReporterCountry();
        //         Country partner = createPartnerCountry();
        //         Product product = createProduct();
        //         DutyType dutyType = createDutyType();

        //         countryRepo.save(reporter);
        //         countryRepo.save(partner);
        //         productRepo.save(product);
        //         dutyTypeRepo.save(dutyType);

        //         countryRepo.flush();
        //         productRepo.flush();
        //         dutyTypeRepo.flush(); // forces persistence context to track this entity

        //         TariffSchedule schedule = createTariffSchedule(
        //                         reporter,
        //                         partner,
        //                         product,
        //                         dutyType,
        //                         childDuty);

        //         // set bidirectional link
        //         if (childDuty != null) {
        //                 childDuty.setTariffSchedule(schedule);
        //         }

        //         dutyType.getTariffSchedules().add(schedule);
        //         scheduleRepo.save(schedule);
        //         scheduleRepo.flush(); // forces persistence context to track this entity

        //         return schedule;
        // }

        // Unused for manual AuditLog creation in tests
        public static AuditLog createAuditLog(TariffSchedule tariffSchedule) {
                AuditLog log = new AuditLog(
                                TestConstants.AUDIT_LOG_ID, // Null in TestConstants - ID will be set by auto-increment
                                tariffSchedule.getTariffId(),
                                tariffSchedule.getClass().getSimpleName(),
                                TestConstants.AUDIT_CHANGE_TYPE,
                                TestConstants.AUDIT_CHANGED_BY,
                                TestConstants.AUDIT_CHANGE_DATE,
                                TestConstants.AUDIT_CHANGE_DETAILS);
                return log;
        }

        // Unused for manual AuditLog creation in tests
        public static AuditLog createAuditLog(Duty duty) {
                AuditLog log = new AuditLog(
                                null,
                                duty.getTariffSchedule().getTariffId(),
                                duty.getClass().getSimpleName(),
                                TestConstants.AUDIT_CHANGE_TYPE,
                                TestConstants.AUDIT_CHANGED_BY,
                                TestConstants.AUDIT_CHANGE_DATE,
                                TestConstants.AUDIT_CHANGE_DETAILS

                );
                return log;
        }
}
