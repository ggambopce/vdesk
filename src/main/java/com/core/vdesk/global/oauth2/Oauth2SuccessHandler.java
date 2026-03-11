package com.core.vdesk.global.oauth2;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.core.vdesk.global.jwt.AuthCookieUtil;
import com.core.vdesk.global.jwt.TokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class Oauth2SuccessHandler implements AuthenticationSuccessHandler {

    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication authentication) throws IOException, ServletException {
        log.info("Authentication Success 실행");
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        String email = principalDetails.getEmail();
        if (email == null || email.isBlank()) {
            log.error("소셜 로그인 성공 후 이메일이 비어 있음");
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "이메일이 필요합니다.");
            return;
        }

        // AT/RT 발급
        var pair = tokenService.issueAll(email);
        // 동일 포맷 쿠키 세팅 (HttpOnly, SameSite 등 일괄 관리)
        AuthCookieUtil.writeAuthCookies(res, pair.at(), pair.rt(), isHttps(req));

        // 로그인 후 JSON 응답
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json;charset=UTF-8");

        String redirectUri = getFrontendSuccessRedirect(req);

        log.info("OAuth2 로그인 성공 → redirect: {}", redirectUri);
        res.sendRedirect(redirectUri);

    }

    private boolean isHttps(HttpServletRequest req) {
        String proto = req.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(proto) || req.isSecure();
    }

    private String getFrontendSuccessRedirect(HttpServletRequest req) {
        // 운영 환경
        if (isHttps(req)) {
            return "https://matatabi-pkbe.vercel.app/auth/callback";
        }
        // 로컬 개발
        return "http://localhost:5174/auth/callback";
    }

}