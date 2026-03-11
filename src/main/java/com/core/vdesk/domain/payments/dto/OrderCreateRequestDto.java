package com.core.vdesk.domain.payments.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreateRequestDto {

    @NotBlank
    private String productCode;     // BASIC_PLAN, STANDARD_PLAN, BUSINESS_PLAN

    @NotNull
    @Min(1) @Max(100)
    private Integer quantity;       // PC 수량

    @NotNull
    @Min(1) @Max(12)
    private Integer durationMonths; // 사용기간(1, 3, 6, 9, 12) — 일반결제용
}
