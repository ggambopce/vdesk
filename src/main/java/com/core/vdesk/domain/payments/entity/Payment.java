package com.core.vdesk.domain.payments.entity;

import com.core.vdesk.domain.payments.enums.PayProvider;
import com.core.vdesk.domain.payments.enums.TxStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Setter
@Getter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "orders_id",
            referencedColumnName = "id",
            nullable = false
    )
    private Orders order;

    @Enumerated(EnumType.STRING)
    private PayProvider provider;

    private String currency;

    /**
     * [PG 공통 결제 트랜잭션 키]
     * - WELCOME : tid
     * - PAYPAL  : captureId
     * - PADDLE  : transactionId
     * - WELCOME_BILLING : tid
     */
    @Column(name = "pg_payment_key", length = 256, unique = true)
    private String pgPaymentKey;

    /** 빌링 결제 charge 시 사용한 billingKey */
    @Column(name = "billing_key", length = 100)
    private String billingKey;

    /** 페이팔, 패들 구독 결제시 구독 ID */
    @Column(name = "subscription_id", length = 100)
    private String subscriptionId;

    /** 승인된 금액(원) */
    @Column(name = "paid_amount")
    private Long paidAmount;

    @Column(name = "paid_usd_amount")
    private BigDecimal paidUsdAmount;

    /** 승인 시각 */
    @Column(name = "approved_at")
    private Instant approvedAt;

    /** 결제 트랜잭션 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TxStatus status;

    /** PG 원문 응답 저장용 */
    @Lob
    @Column(name = "raw_response", columnDefinition = "LONGTEXT")
    private String rawResponse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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
