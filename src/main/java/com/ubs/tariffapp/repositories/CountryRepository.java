package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.Country;

public interface CountryRepository extends JpaRepository<Country, String> {
}
