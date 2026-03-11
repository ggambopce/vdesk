package com.core.vdesk.domain.welcome.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.core.vdesk.domain.welcome.dto.WelcomeBillingChargeRequestDto;
import com.core.vdesk.domain.welcome.dto.WelcomeBillingChargeResponseDto;
import com.core.vdesk.domain.welcome.dto.WelcomeBillingProps;
import com.core.vdesk.domain.welcome.util.WelcomePayApiCrypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeBillingClient {

    private final WelcomeBillingProps props;

    private WebClient client() {
        return WebClient.builder()
                .baseUrl(props.getPayApiBaseUrl())
                .defaultHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .build();
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public String approveRaw(String reqUrl, String mid, String tid) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("P_MID", mid);
        body.add("P_TID", tid);
        body.add("charset", "EUC-KR");
        body.add("format", "JSON");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            String raw = restTemplate.postForObject(reqUrl, new HttpEntity<>(body, headers), String.class);
            if (raw == null || raw.isBlank()) throw new IllegalStateException("approve response is empty");
            log.info("[WELCOME APPROVE] reqUrl={}, tid={}, raw={}", reqUrl, tid, raw);
            return raw;
        } catch (Exception e) {
            log.warn("[WELCOME APPROVE] failed. reqUrl={}, tid={}, msg={}", reqUrl, tid, e.getMessage());
            throw e;
        }
    }

    public WelcomeBillingChargeResponseDto billpay(WelcomeBillingChargeRequestDto req) {
        String mid = props.getMid();
        String mkey = WelcomePayApiCrypto.mkey(props.getSignKey());

        String timestamp = WelcomePayApiCrypto.now14();
        String price = nz(req.getPrice());
        if (price.isBlank()) throw new IllegalArgumentException("price is required");

        String oid = nz(req.getOid());
        if (oid.isBlank()) oid = mid + "_" + System.currentTimeMillis();

        String billkey = nz(req.getBillkey());
        if (billkey.isBlank()) throw new IllegalArgumentException("billkey is required");

        String signatureSource = WelcomePayApiCrypto.billpaySignatureSource(mid, mkey, oid, price, timestamp);
        String signature = WelcomePayApiCrypto.signatureSha256(signatureSource);

        Map<String, String> form = new LinkedHashMap<>();
        // 문서 필드명 그대로(소문자)
        form.put("mid", mid);
        form.put("oid", oid);
        if (!nz(req.getGoodsName()).isBlank()) form.put("goodsName", req.getGoodsName());
        form.put("price", price);
        if (!nz(req.getBuyerName()).isBlank()) form.put("buyerName", req.getBuyerName());
        if (!nz(req.getBuyerTel()).isBlank()) form.put("buyerTel", req.getBuyerTel());
        if (!nz(req.getBuyerEmail()).isBlank()) form.put("buyerEmail", req.getBuyerEmail());
        form.put("billkey", billkey);
        form.put("cardQuota", nz(req.getCardQuota()).isBlank() ? "00" : req.getCardQuota());
        form.put("quotaInterest", nz(req.getQuotaInterest()).isBlank() ? "0" : req.getQuotaInterest());
        form.put("timestamp", timestamp);
        form.put("signature", signature);

        String requestBody = toFormUrlEncoded(form);

        String path = "/billing/billpay";
        String url = props.getPayApiBaseUrl() + path;

        String raw = client().post()
                .uri(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        WelcomeBillingChargeResponseDto res = new WelcomeBillingChargeResponseDto();
        res.setRequestUrl(url);
        res.setRequestBody(requestBody);
        res.setSignatureSource(signatureSource);
        res.setRawResponse(raw);

        log.info("[PAYAPI BILLPAY] url={}, oid={}, price={}, resultRaw={}", url, oid, price, raw);
        return res;
    }

    private static String toFormUrlEncoded(Map<String, String> m) {
        return m.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String enc(String s) {
        return URLEncoder.encode(nz(s), StandardCharsets.UTF_8);
    }

    private static String nz(String s) { return s == null ? "" : s; }

}
