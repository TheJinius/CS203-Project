package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.Tariff;

public interface TariffRepository extends JpaRepository<Tariff, String> {
    // Add custom query methods later if needed
}