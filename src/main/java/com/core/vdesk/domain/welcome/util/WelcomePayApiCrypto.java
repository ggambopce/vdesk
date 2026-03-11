package com.core.vdesk.domain.welcome.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class WelcomePayApiCrypto {
    private WelcomePayApiCrypto() {}

    // PayAPI timestamp: YYYYMMDDhhmmss (14)
    public static String now14() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    // mkey = sha256(signKey)
    public static String mkey(String signKey) {
        return sha256Hex(signKey);
    }

    // signatureSource: 필드명 소문자 + 알파벳순 + key=value&... (마지막 & 없음, 공백 없음)
    // billpay: mid+mkey+oid+price+timestamp 대상
    public static String billpaySignatureSource(String mid, String mkey, String oid, String price, String timestamp) {
        // 알파벳순: mid, mkey, oid, price, timestamp
        return "mid=" + mid
                + "&mkey=" + mkey
                + "&oid=" + oid
                + "&price=" + price
                + "&timestamp=" + timestamp;
    }

    public static String signatureSha256(String signatureSource) {
        return sha256Hex(signatureSource);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format(Locale.ROOT, "%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("sha256 failed", e);
        }
    }
}

