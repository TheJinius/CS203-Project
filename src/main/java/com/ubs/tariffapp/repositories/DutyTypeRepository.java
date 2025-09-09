package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.DutyType;
import com.ubs.tariffapp.models.DutyTypeId;

public interface DutyTypeRepository extends JpaRepository<DutyType, DutyTypeId> {
}
