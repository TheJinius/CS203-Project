package com.ubs.tariffapp.duty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ubs.tariffapp.duty.Duty;

import java.util.*;

@Repository
public interface DutyRepository extends JpaRepository<Duty, Long> {
    // Built-in methods you get for free:
    // findById(Long id)
    // findAll()
    // save(Tariff tariff)
    // deleteById(Long id)
    
    // Custom query methods you might want to add:
    Duty findByTariffCode(String tariffCode);
    List<Duty> findByStatus(String status);
    List<Duty> findByTariffType(String tariffType);
}