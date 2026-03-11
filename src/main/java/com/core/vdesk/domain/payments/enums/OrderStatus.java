package com.core.vdesk.domain.payments.enums;

public enum OrderStatus {
    PENDING,        // 결제 시도 중 (폴링 대상)
    PAID,           // 결제 성공이 확인됨 (Payment 기준으로 확정)
    FAILED,         // 결제 시도 후 실패 (웹훅 실패 수신)
    CANCELLED,      // 사용자가 결제 흐름을 중단함 (닫기, 뒤로가기)
}
