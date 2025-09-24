package com.ubs.tariffapp.repositories;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ubs.tariffapp.models.TariffSchedule;

public interface TariffScheduleRepository extends JpaRepository<TariffSchedule, Integer> {
    
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
}
