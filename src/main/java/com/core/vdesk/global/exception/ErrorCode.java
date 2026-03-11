package com.core.vdesk.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 400 Bad Request
    INVALID_REQUEST(400, HttpStatus.BAD_REQUEST, "요청 형식이 다릅니다."),
    // 401 Unauthorized
    UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    // 403 Forbidden
    FORBIDDEN(403, HttpStatus.FORBIDDEN, "권한이 없습니다."),
    // 404 Not Found
    NOT_FOUND(404, HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."),
    // 500 Internal Server Error
    INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 페이먼츠 관련 에러
    PAYMENTS_API_HTTP_ERROR(502, HttpStatus.BAD_GATEWAY, "결제 서버 오류."),
    PAYMENTS_API_COMMUNICATION_ERROR(502, HttpStatus.BAD_GATEWAY, "결제 서버 통신 실패."),
    PAYMENTS_API_RESPONSE_PARSE_ERROR(502, HttpStatus.BAD_GATEWAY, "결제 응답 파싱 실패."),
    PRODUCT_NOT_FOUND(404, HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    ORDER_NOT_FOUND(404, HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    PLAN_NOT_FOUND(404, HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다."),
    INVALID_PLAN_STATUS(400, HttpStatus.BAD_REQUEST, "현재 상태에서 지원하지 않는 작업입니다."),
    PAYMENT_AMOUNT_MISMATCH(400, HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    DUPLICATE_PAYMENT(409, HttpStatus.CONFLICT, "이미 처리된 결제입니다.");

    private int code;
    private HttpStatus status;
    private String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

}
