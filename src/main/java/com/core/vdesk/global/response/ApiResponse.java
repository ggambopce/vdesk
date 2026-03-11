package com.core.vdesk.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final int code;
    private final String message;
    private final T result;

    /**
     * 성공 응답 헬퍼 메서드
     * - HTTP 200 응답을 가정하고 code=200으로 설정.
     * - 성공 시 공통적으로 사용할 수 있는 정적 팩토리 메서드.
     */
    public static <T> ApiResponse<T> ok(String message,T result){
        return new ApiResponse<>(200, message, result);
    }

    /**
     * 실패 응답 헬퍼 메서드
     * - BusinessException 을 사용하지 않고 컨트롤러에서 직접 실패 응답을 내려야 할 때 사용.
     * - ErrorCode 대신 임의의 code, message 를 설정할 수 있음.
     */
    public static <T> ApiResponse<T> failure(int code, String message, T result){
        return new ApiResponse<>(code, message, result);
    }
}
