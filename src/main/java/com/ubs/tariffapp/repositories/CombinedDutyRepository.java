package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.duty.CombinedDuty;

public interface CombinedDutyRepository extends JpaRepository<CombinedDuty, Integer> {
}
