package com.core.vdesk.global.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Oauth2FailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res, AuthenticationException exception) throws IOException, ServletException {

        log.warn("소셜 로그인 실패: {}", exception.getMessage());

        // 앱에 넘길 에러 메시지
        String errorMsg = "소셜 로그인을 실패하였습니다.";
        // URL 인코딩 (공백/한글 대비)
        String encoded = URLEncoder.encode(errorMsg, StandardCharsets.UTF_8);

        // 프로그램용 스캠링크 리다이렉트
        String redirectUri = "http://localhost:57403/auth?error=" + encoded;

        log.info("소셜 로그인 실패 스킴 리다이렉트: {} ", redirectUri);
        res.sendRedirect(redirectUri);


    }
}