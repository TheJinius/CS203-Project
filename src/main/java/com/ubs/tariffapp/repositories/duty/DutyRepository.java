package com.ubs.tariffapp.repositories.duty;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.duty.Duty;

public interface DutyRepository extends JpaRepository<Duty, Integer> {
    
    // If you need to find by tariff schedule, use this simple method:
    Optional<Duty> findByTariffSchedule_TariffId(Integer tariffId);
}
