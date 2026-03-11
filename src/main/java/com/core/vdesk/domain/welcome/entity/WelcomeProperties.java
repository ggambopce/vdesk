package com.core.vdesk.domain.welcome.entity;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "welcome")
@Getter
@Setter
public class WelcomeProperties {

    /**
     * 가맹점 ID (mid)
     * 예: welcometst, 실제 운영 MID
     */
    private String mid;

    /**
     * 웹표준 signKey (관리자 > 상점정보 > 계약정보 > 부가정보 > 웹결제 signkey)
     */
    private String signKey;

    /**
     * signKey 를 SHA-256 으로 해시한 값 (mKey)
     * 애플리케이션 시작 시 계산해 두고 사용
     */
    private String mKey;

    /**
     * 우리 서비스 도메인 (https://sandboxie.co.kr 같은 것)
     */
    private String siteDomain;

    /**
     * 인증결과 리턴 URL path (컨트롤러 매핑과 맞춰야 함)
     * 예: /api/payments/welcome/return
     */
    private String returnPath;

    /**
     * 결제창 닫기용 URL path (샘플 close.jsp 역할)
     * 예: /payments/welcome/close
     */
    private String closePath;

    @PostConstruct
    public void init() {
        if (mKey == null || mKey.isBlank()) {
            this.mKey = sha256(this.signKey);
        }
    }

    private String sha256(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
