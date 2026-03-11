package com.core.vdesk.domain.payments.entity;

import com.core.vdesk.domain.payments.enums.OrderStatus;
import com.core.vdesk.domain.payments.enums.OrderType;
import com.core.vdesk.domain.users.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 우리 내부 주문번호(외부 노출용) — 유니크
    @Column(name = "order_id", nullable = false, length = 64, unique = true)
    private String orderId;

    // Product FK (N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Users FK 연결
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    // 결제 금액(원)
    @Column
    private Long amount;

    // 구매 수량 (VM 대수)
    @Column
    private Integer quantity;

    // 사용기간(개월) - 일반결제용
    @Column(name = "duration_months")
    private Integer durationMonths;

    // 결제수단 코드(예: CARD, VIRTUAL_ACCOUNT, TRANSFER 등)
    @Column(nullable = false, length = 32)
    private String payMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderType type;     // GENERAL(일반결제), BILLING(빌링결제)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status; // PENDING, PAID, FAILED, CANCELLED

    @Column(length = 255)
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (payMethod == null) payMethod = "unknown";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
