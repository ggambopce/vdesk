package com.core.vdesk.domain.welcome.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.core.vdesk.domain.welcome.dto.WelcomeBillingTargetDto;
import com.core.vdesk.domain.welcome.entity.WelcomeBillingKey;

public interface WelcomeBillingKeyRepository extends JpaRepository<WelcomeBillingKey, Long> {
    Optional<WelcomeBillingKey> findByBillingKey(String billingKey);

    Optional<WelcomeBillingKey> findByUser_UserId(Long userId);

    @Query("""
        SELECT new com.core.vdesk.domain.welcome.dto.WelcomeBillingTargetDto(
            u.userId,
            wbk.billingKey,
            wbk.status,
            up.nextChargeDate,
            up.paymentStatus
        )
        FROM UserPlan up
        JOIN up.user u
        JOIN up.payment p
        JOIN WelcomeBillingKey wbk ON wbk.user.userId = u.userId
        WHERE up.paymentStatus = com.core.vdesk.domain.payments.enums.PaymentStatus.ACTIVE
          AND wbk.status = com.core.vdesk.domain.welcome.entity.BillingKeyStatus.ACTIVE
          AND p.provider = com.core.vdesk.domain.payments.enums.PayProvider.WELCOME_BILLING
          AND up.nextChargeDate IS NOT NULL
          AND up.nextChargeDate <= :now
    """)
    List<WelcomeBillingTargetDto> findWelcomeBillingTargets(@Param("now") Instant now);
}
