package com.core.vdesk.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * ErrorCode 기본 메시지를 사용하는 생성자.
     * 예)throw new BusinessException(ErrorCode.NOT_FOUND);
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode 와 별도의 커스텀 메시지를 함께 사용하는 생성자.
     * 예) throw new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 사용자입니다.");
     * GlobalExceptionHandler 에서
     * - ErrorCode 기본 메시지와 다르면 커스텀 메시지를 우선 사용하도록 처리.
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 원인 예외(cause)를 함께 전달하는 생성자.
     * - 외부 라이브러리/DB 예외를 감싸서 비즈니스 예외로 변환할 때 사용.
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * ErrorCode + 커스텀 메시지 + 원인 예외를 모두 전달하는 생성자.
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}