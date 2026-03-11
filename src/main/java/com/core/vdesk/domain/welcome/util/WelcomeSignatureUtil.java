package com.core.vdesk.domain.welcome.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

import com.core.vdesk.domain.welcome.entity.WelcomeProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WelcomeSignatureUtil {

    private final WelcomeProperties props;

    /**
     * 결제요청용 signature 생성
     * Target : mKey, oid, price, timestamp
     */
    public String createRequestSignature(String oid, String price, String timestamp) {
        String signParam = "mKey=" + props.getMKey()
                + "&oid=" + oid
                + "&price=" + price
                + "&timestamp=" + timestamp;
        return sha256(signParam);
    }

    /**
     * 승인요청용 signature 생성
     * Target : authToken, timestamp
     */
    public String createApproveSignature(String authToken, String timestamp) {
        String signParam = "authToken=" + authToken
                + "&timestamp=" + timestamp;
        return sha256(signParam);
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
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
