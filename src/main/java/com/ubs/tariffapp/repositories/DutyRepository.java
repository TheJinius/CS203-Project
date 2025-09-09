package com.ubs.tariffapp.repositories;

import com.ubs.tariffapp.models.Duty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DutyRepository extends JpaRepository<Duty, Long> {

    @Query("SELECT d FROM Duty d WHERE d.tariffSchedule.tariffId = (" +
           "SELECT ts.tariffId FROM TariffSchedule ts " +
           "WHERE ts.reporterCountry = :countryAId " +
           "AND ts.reporterCountryB = :countryBId " +
           "AND ts.productCode = :productCode)")
    Optional<Duty> findDutyByCountriesAndProduct(
        @Param("countryAId") String countryAId,
        @Param("countryBId") String countryBId,
        @Param("productCode") String productCode
    );
}