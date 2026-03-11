package com.core.vdesk.domain.payments.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.core.vdesk.domain.payments.entity.Orders;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Orders, Integer> {
    Optional<Orders> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Orders o where o.orderId = :orderId")
    Optional<Orders> findForUpdateByOrderId(@Param("orderId") String orderId);
}
