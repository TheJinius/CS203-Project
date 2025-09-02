package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.User;

// JpaRepository extends CrudRepository, and provides additional JPA-specific methods
// Recall CrudRepository provides the implementation for basic CRUD operations
public interface UserRepository extends JpaRepository<User, String> {
    // Add custom query methods later if needed
}