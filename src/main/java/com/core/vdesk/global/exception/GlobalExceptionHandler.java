package com.core.vdesk.global.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * - 서비스/도메인 레이어에서 명시적으로 던진 BusinessException을 처리한다.
     * - ErrorCode 를 기반으로 HTTP 상태 코드와 에러 정보를 구성하고 ErrorResponseDto 형태로 응답한다.
     * 예) throw new BusinessException(ErrorCode.NOT_FOUND, "게시글을 찾을 수 없습니다.");
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(BusinessException e, HttpServletRequest req) {
        ErrorCode code = e.getErrorCode();

        // Exception에 커스텀 메시지가 있으면 사용, 없으면 기본 메시지 사용
        String message = (e.getMessage() != null && !e.getMessage().equals(code.getMessage()))
                ? e.getMessage()
                : code.getMessage();

        ErrorResponseDto body = new ErrorResponseDto(code, message);

        return ResponseEntity
                .status(code.getStatus())
                .body(body);
    }
}

