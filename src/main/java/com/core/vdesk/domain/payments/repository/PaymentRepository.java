package com.core.vdesk.domain.payments.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.core.vdesk.domain.payments.entity.Payment;
import com.core.vdesk.domain.payments.enums.TxStatus;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    boolean existsPaymentByPgPaymentKey(String pgPaymentKey);

    boolean existsByPgPaymentKey(String tid);

    @Query("""
        select coalesce(sum(p.paidAmount), 0)
        from Payment p
        where p.status = :status
          and p.approvedAt >= :from
          and p.approvedAt < :to
    """)
    Long sumPaidKrwAmount(
            @Param("status") TxStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    boolean existsByOrder_OrderIdAndStatus(String orderId, TxStatus txStatus);
}

