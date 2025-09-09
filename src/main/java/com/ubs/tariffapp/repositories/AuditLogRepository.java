package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
