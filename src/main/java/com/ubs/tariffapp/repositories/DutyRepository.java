package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.duty.Duty;

public interface DutyRepository extends JpaRepository<Duty, Integer> {
    // You can add custom query methods here if needed
}
