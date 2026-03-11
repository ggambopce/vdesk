package com.core.vdesk.global.jwt;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwt;

    public record Pair(String at, String rt) {}

    /** 최초 발급: AT/RT 동시 발급 (RT는 회전하지 않음) */
    public Pair issueAll(String email) {
        String at = jwt.generateAccessToken(email);
        String rt = jwt.generateRefreshToken(email); // 긴 수명. 저장/회전 없음
        log.info("RT ISSUE email={} ttl={}h", email, jwt.getRefreshTokenExpiration() / 3600000);
        return new Pair(at, rt);
    }

    /** 무상태 리프레시: RT 검증 → 새 AT만 발급 (RT는 그대로 유지, 회전 없음) */
    public Pair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank())
            throw new IllegalArgumentException("리프레시 토큰 누락");

        if (!jwt.validateToken(refreshToken))
            throw new IllegalStateException("유효하지 않은 리프레시 토큰");

        TokenMeta meta = jwt.parse(refreshToken);
        if (meta.getType() != TokenType.REFRESH)
            throw new IllegalArgumentException("토큰 타입이 REFRESH가 아님");
        if (meta.isExpired())
            throw new IllegalStateException("만료된 리프레시 토큰");

        String newAt = jwt.generateAccessToken(meta.getEmail());
        // RT는 회전/재발급하지 않는다 → null 반환
        log.info("RT REFRESH(email={}) -> new AT only", meta.getEmail());
        return new Pair(newAt, refreshToken);
    }

    /** 로그아웃: fam 포인터 제거(현재 기기/브라우저 세션 종료) */
    public void logoutByRefreshToken(String refreshToken) {
        log.info("쿠키 삭제로 세션 종료");
    }

}