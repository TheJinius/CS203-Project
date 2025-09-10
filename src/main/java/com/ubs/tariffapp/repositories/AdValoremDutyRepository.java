package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.duty.AdValoremDuty;

public interface AdValoremDutyRepository extends JpaRepository<AdValoremDuty, Integer> {
}
