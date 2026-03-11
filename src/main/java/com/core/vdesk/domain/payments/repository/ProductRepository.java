package com.core.vdesk.domain.payments.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.core.vdesk.domain.payments.entity.Product;
import com.core.vdesk.domain.payments.enums.ProductCode;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Optional<Product> findByProductCode(ProductCode productCode);
}

