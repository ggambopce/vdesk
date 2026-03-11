package com.core.vdesk.domain.payments.enums;

/**
 * 거래 상태
 * 금전 거래 처리 결과
 * 거래 승인, 거래취소(환불), 거래실패
 */
public enum TxStatus {
    APPROVED,
    CANCELLED,
    FAILED
}
