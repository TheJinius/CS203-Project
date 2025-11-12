package com.ubs.tariffapp.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ubs.tariffapp.models.Country;
import com.ubs.tariffapp.models.TariffSchedule;
import com.ubs.tariffapp.models.dto.TariffSearchResult;

public interface TariffScheduleRepository extends JpaRepository<TariffSchedule, Integer> {
    
    // Existing method - keep for backward compatibility
    @Query("SELECT ts FROM TariffSchedule ts " +
           "WHERE ts.reporter.countryId = :reporterCode " +
           "AND ts.partner.countryId = :partnerCode " +
           "AND ts.product.tlCode = :tlCode")
    Optional<TariffSchedule> findByReporterAndPartnerAndTl(
        @Param("reporterCode") String reporterCode,
        @Param("partnerCode") String partnerCode,
        @Param("tlCode") String tlCode
    );
    
    @Query("SELECT ts FROM TariffSchedule ts " +
           "WHERE ts.reporter.countryId = :reporterCode " +
           "AND ts.partner.countryId = :partnerCode " +
           "AND ts.product.tlCode = :tlCode " +
           "AND ts.tariffYear = :year")
    Optional<TariffSchedule> findByReporterAndPartnerAndTlAndYear(
        @Param("reporterCode") String reporterCode,
        @Param("partnerCode") String partnerCode,
        @Param("tlCode") String tlCode,
        @Param("year") int year
    );

    @Query("SELECT ts FROM TariffSchedule ts " +
           "WHERE ts.reporter.countryId = :reporterCode " +
           "AND ts.partner.countryId = :partnerCode " +
           "AND ts.product.tlCode = :tlCode " +
           "AND ts.tariffYear = :year")
    List<TariffSchedule> findAllByReporterAndPartnerAndTlAndYear(
        @Param("reporterCode") String reporterCode,
        @Param("partnerCode") String partnerCode,
        @Param("tlCode") String tlCode,
        @Param("year") int year
    );

    // SIMPLIFIED: Primary options query - using only available TariffSchedule fields
    @Query("""
        SELECT new com.ubs.tariffapp.models.dto.TariffSearchResult(
            ts.tariffId,
            CONCAT('Duty Info: ', COALESCE(ts.note, 'No description')),
            ts.tlsSuffix,
            CAST('N/A' AS string),
            CAST('N/A' AS string),
            ts.tariffYear
        )
        FROM TariffSchedule ts
        WHERE ts.reporter.countryId = :reporter
        AND ts.partner.countryId = :partner
        AND ts.product.tlCode = :tlCode
        AND ts.tariffYear = :year
        ORDER BY ts.tlsSuffix NULLS FIRST
        """)
    List<TariffSearchResult> findOptions(
        @Param("reporter") String reporter,
        @Param("partner") String partner,
        @Param("tlCode") String tlCode,
        @Param("year") Integer year
    );

    // SIMPLIFIED: Fallback A - World partner
    @Query("""
        SELECT new com.ubs.tariffapp.models.dto.TariffSearchResult(
            ts.tariffId,
            CONCAT('Duty Info: ', COALESCE(ts.note, 'No description')),
            ts.tlsSuffix,
            CAST('N/A' AS string),
            CAST('N/A' AS string),
            ts.tariffYear
        )
        FROM TariffSchedule ts
        WHERE ts.reporter.countryId = :reporter
        AND ts.partner.countryId = '000'
        AND ts.product.tlCode = :tlCode
        AND ts.tariffYear = :year
        ORDER BY ts.tlsSuffix NULLS FIRST
        """)
    List<TariffSearchResult> findWorldOptions(
        @Param("reporter") String reporter,
        @Param("tlCode") String tlCode,
        @Param("year") Integer year
    );
    
    // Get distinct years from tariff_schedule table
    @Query("SELECT DISTINCT ts.tariffYear FROM TariffSchedule ts ORDER BY ts.tariffYear DESC")
    List<Integer> findDistinctYears();
    
    // Get distinct countries that have at least one tariff (from both reporter and partner columns)
    @Query("SELECT DISTINCT c FROM Country c WHERE c.countryId IN " +
           "(SELECT DISTINCT ts.reporter.countryId FROM TariffSchedule ts " +
           "UNION " +
           "SELECT DISTINCT ts.partner.countryId FROM TariffSchedule ts) " +
           "ORDER BY c.countryName")
    List<Country> findDistinctCountries();
}
