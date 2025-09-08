package com.ubs.tariffapp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ubs.tariffapp.models.Product;

public interface ProductRepository extends JpaRepository<Product, String> {
}
