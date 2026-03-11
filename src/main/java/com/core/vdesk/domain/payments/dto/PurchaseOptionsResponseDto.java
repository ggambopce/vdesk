package com.core.vdesk.domain.payments.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOptionsResponseDto {

    private List<PaymentTypeDto> paymentTypes;
    private List<Integer> availableDurations;
    private Integer maxQuantity;
    private Integer defaultQuantity;
    private Integer defaultDurationMonths;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentTypeDto {
        private String code;
        private String label;
    }
}
