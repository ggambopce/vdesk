package com.core.vdesk.domain.payments.entity;

import com.core.vdesk.domain.payments.enums.BillingProvider;
import com.core.vdesk.domain.payments.enums.PaymentStatus;
import com.core.vdesk.domain.payments.enums.PlanType;
import com.core.vdesk.domain.users.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class UserPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // 구독 주인의 Users FK
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // 마지막 결제 Payment FK (가장 최근 결제 내역 참조)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_provider", length = 20)
    private BillingProvider billingProvider; // WELCOME, PAYPAL, PADDLE

    private String billingId;               // WELCOME은 billingKey, PAYPAL/PADDLE은 subscriptionId
    private Instant expiresAt;              // 이용 만료 시간 FREE는 null
    private Instant nextChargeDate;         // 스케쥴러 타겟

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;    // ACTIVE, FAILED, CANCELLED, PAUSED, EXPIRED

    @Column(name = "addon_session_count", nullable = false)
    private int addonSessionCount = 0;

    @Column(name = "addon_expires_at")
    private Instant addonExpiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
