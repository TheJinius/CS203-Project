package com.ubs.tariffapp.repositories.duty;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.duty.SpecificDuty;

public interface SpecificDutyRepository extends JpaRepository<SpecificDuty, Integer> {
}
