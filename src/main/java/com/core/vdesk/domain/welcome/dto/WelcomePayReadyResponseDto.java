package com.core.vdesk.domain.welcome.dto;

import lombok.Builder;

@Builder
public record WelcomePayReadyResponseDto(
        // 요약 정보 (명세서 추가 필드)
        String paymentType,
        String productCode,
        String productName,
        Integer quantity,
        Integer durationMonths,
        Long unitMonthlyAmount,
        Long totalAmount,
        // 웰컴 결제창 파라미터
        String version,
        String mid,
        String oid,
        String goodname,
        String price,
        String currency,
        String buyername,
        String buyertel,
        String buyeremail,
        String timestamp,
        String signature,
        String returnUrl,
        String closeUrl,
        String mKey,
        String gopaymethod,
        String charset,
        String payViewType
) {}
