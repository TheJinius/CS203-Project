package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.TariffSchedule;

public interface TariffScheduleRepository extends JpaRepository<TariffSchedule, Integer> {
}
