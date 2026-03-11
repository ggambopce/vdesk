package com.core.vdesk.domain.welcome.dto;

import java.time.Instant;

import com.core.vdesk.domain.payments.enums.PaymentStatus;
import com.core.vdesk.domain.welcome.entity.BillingKeyStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WelcomeBillingTargetDto {
    private Long userId;
    private String billingKey;
    private BillingKeyStatus status;
    private Instant nextChargeDate;
    private PaymentStatus paymentStatus;
}
