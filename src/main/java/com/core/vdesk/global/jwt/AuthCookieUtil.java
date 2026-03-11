package com.core.vdesk.global.jwt;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AuthCookieUtil {

    /**
     * AccessToken(AT) + RefreshToken(RT) 둘 다 쿠키에 저장하는 메서드.
     * 주로 로그인 성공 시 호출된다.
     * @param res     HttpServletResponse
     * @param at      Access Token
     * @param rt      Refresh Token
     * @param secure  HTTPS 에서만 사용하도록 할지 여부
     * SameSite 규칙:
     *  - 단일 도메인(웹앱 + API 동일 도메인) 환경이면 "Strict" 가 가장 안전
     *  - cross-domain 환경(프론트와 백엔드 도메인이 다름) 또는 소셜로그인 위젯 연동 시 "None"
     */
    public static void writeAuthCookies(HttpServletResponse res, String at, String rt, boolean secure) {
        // 운영에서 크로스도메인/위젯 연동 필요하면 sameSite("None") + secure=true
        // 단일 도메인만 쓰면 "Strict"가 CSRF 측면에서 더 안전
        add(res, "AT", at, Duration.ofDays(365), secure, "Strict");
        add(res, "RT", rt, Duration.ofDays(365), secure, "Strict");
    }
    /**
     * AccessToken 만 재발급하여 쿠키에 설정.
     * RefreshToken 은 그대로 유지하는 “무상태 리프레시” 구조에서 사용됨.
     */
    public static void writeAtCookie(HttpServletResponse res, String at, boolean secure) {
        add(res, "AT", at, Duration.ofDays(365), secure, "Strict");
    }

    /**
     * 로그아웃 시 AT/RT 모두 삭제.
     * Max-Age=0 으로 동일 설정의 쿠키를 덮어써야 클라이언트에서 정확히 제거됨.
     */
    public static void clearAuthCookies(HttpServletResponse res, boolean secure) {
        add(res, "AT", "", Duration.ZERO, secure, "Strict");
        add(res, "RT", "", Duration.ZERO, secure, "Strict");
    }

    /**
     * 특정 쿠키를 지정된 domain/path 옵션으로 삭제.
     * (쿠키 삭제 시 domain/path/sameSite 가 동일해야 제거됨)
     */
    public static void deleteCookie(HttpServletResponse res, String name,
                                    String domain, String path, boolean secure, String sameSite) {
        // Max-Age=0 + 동일 도메인/경로/SameSite/Secure 조합으로 내려야 삭제됨
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .domain(domain)
                .path(path)
                .sameSite(sameSite)
                .maxAge(Duration.ZERO)    // = 0
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static String readCookie(HttpServletRequest req, String name) {
        if (req.getCookies()==null) return null;
        for (var c : req.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    // 내부 공통
    private static void add(HttpServletResponse res, String name, String value,
                            Duration maxAge, boolean secure, String sameSite) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}