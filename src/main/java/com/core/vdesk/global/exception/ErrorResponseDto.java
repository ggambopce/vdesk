package com.core.vdesk.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponseDto {
    private final int code;          // 숫자 코드
    private final String status;     // HttpStatus 이름 (예: "BAD_REQUEST")
    private final String message;    // 예외 메시지

    /**
     * ErrorCode + 메시지 기반 생성자
     * - ErrorCode 에 정의된 code, status 를 자동으로 세팅하고
     *   메시지는 핸들러에서 결정된 문자열을 그대로 사용.
     */
    public ErrorResponseDto(ErrorCode errorCode, String message) {
        this.code = errorCode.getCode();
        this.status = errorCode.getStatus().toString();
        this.message = message;
    }
}
