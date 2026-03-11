package com.core.vdesk.domain.payments.enums;

public enum PaymentStatus {
    ACTIVE,                 // 정상 구독중
    EXPIRED,                // 만료
    CANCEL_SCHEDULED,       // 사용자 구독 해지
    CANCELLED,              // 관리자 취소
    PAUSED,                 // 일시중지, 소프트취소
    FAILED,                 // 스케줄러 결제 실패
}
