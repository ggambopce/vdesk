package com.core.vdesk.domain.welcome.entity;

import java.time.Instant;

import com.core.vdesk.domain.users.Users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class WelcomeBillingKey {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private Users user;

    @Column(nullable = false, length = 80)
    private String billingKey; // P_CARD_BILLKEY 저장

    @Column(length = 32)
    private String cardCode; // P_FN_CD1 (선택)

    @Column(length = 64)
    private String cardNumHash; // P_CARD_NUMHASH (선택)

    @Enumerated(EnumType.STRING)
    private BillingKeyStatus status; // ACTIVE, INACTIVE, DELETED

    @Lob
    private String lastResultRaw; // 원문 저장(디버깅)

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
