package com.core.vdesk.global.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Getter
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * 인증 요청 시 사용되는 AccessToken을 생성한다.
     * type = "ACCESS" 클레임을 포함하며, 비교적 짧은 만료 시간으로 설정한다.
     * @param email 토큰의 인증 주체(subject)
     */
    public String generateAccessToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpiration);
        return Jwts.builder()
                .subject(email)
                .claim("type", TokenType.ACCESS.name())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * AT 재발급을 위해 사용되는 RefreshToken을 생성한다.
     * type = "REFRESH" 클레임을 포함하며, 보다 긴 만료 시간을 갖는다.
     * @param email 토큰의 인증 주체(subject)
     */
    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenExpiration);
        return Jwts.builder()
                .subject(email)
                .claim("type", TokenType.REFRESH.name())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰의 서명·형식이 정상이면 true, 위조·손상·파싱 오류가 있을 경우 false를 반환한다.
     * @param token JWT 문자열
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validate fail: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰의 만료 여부를 확인한다.
     * @param token JWT 문자열
     * @return true = 만료됨, false = 유효함
     */
    public boolean isExpired(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return c.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 주어진 JWT를 파싱하여 핵심 클레임 정보를 구조화된 형태(TokenMeta)로 반환한다.
     * 이 메서드는 JWT에 대한 단일 진입점 역할을 수행하며,
     * 토큰 검증과 파싱을 한 번만 수행하여 이메일(subject), 토큰 타입(type),
     * 발급 시각(issuedAt), 만료 시각(expiration)을 함께 반환한다.
     * @param token 파싱할 JWT 문자열
     * @return TokenMeta(email, type, issuedAt, expiration)
     */
    public TokenMeta parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String email = c.getSubject();
        String typeStr = c.get("type", String.class);
        TokenType type = TokenType.valueOf(typeStr);
        return new TokenMeta(email, type, c.getIssuedAt(), c.getExpiration());
    }



}