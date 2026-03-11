package com.core.vdesk.domain.welcome.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class WelcomeMobileSignature {
    private WelcomeMobileSignature() {}

    // sha256 hex
    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // 문서 규칙: mkey=Sha256(signKey)
    public static String mkeyFromSignKey(String signKey) {
        return sha256Hex(signKey);
    }

    // 문서 규칙: "mkey=...&P_AMT=...&P_OID=...&P_TIMESTAMP=..."
    public static String signatureSource(String signKey, String pAmt, String pOid, String pTimestamp) {
        String mkey = mkeyFromSignKey(signKey);

        // 필드명 알파벳순: mkey -> P_AMT -> P_OID -> P_TIMESTAMP
        // 공백/개행/마지막& 절대 금지
        return "mkey=" + mkey
                + "&P_AMT=" + pAmt
                + "&P_OID=" + pOid
                + "&P_TIMESTAMP=" + pTimestamp;
    }

    public static String signature(String signKey, String pAmt, String pOid, String pTimestamp) {
        return sha256Hex(signatureSource(signKey, pAmt, pOid, pTimestamp));
    }
}
