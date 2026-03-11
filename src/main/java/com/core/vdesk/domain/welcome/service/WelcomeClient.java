package com.core.vdesk.domain.welcome.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.core.vdesk.domain.welcome.entity.WelcomeProperties;
import com.core.vdesk.domain.welcome.util.WelcomeSignatureUtil;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WelcomeClient {
    private final WelcomeProperties props;
    private final WelcomeSignatureUtil signatureUtil;

    /**
     * 웰컴 승인 요청 후 응답 원문(raw JSON String)을 반환한다.
     * 설계 포인트:
     * - Client는 "통신"에만 집중하고, 응답 파싱/DB 반영은 Service에서 수행한다.
     * - 승인 API는 application/x-www-form-urlencoded (form POST) 방식이다.
     * - 응답 형식(JSON/XML/NVP)은 요청의 format 파라미터로 제어 가능하다.
     * @param authUrl   인증 결과로 전달받은 승인 API 엔드포인트 (PG가 내려줌)
     * @param authToken 인증 성공 후 내려오는 토큰(승인 요청 검증용)
     * @param price     주문 금액(승인 과정 위변조 검증에 사용되므로 서버 주문 금액을 사용)
     * @return          승인 응답 원문(JSON 문자열). 파싱은 상위 Service에서 진행.
     */
    public String approveRaw(String authUrl, String authToken, String price) {

        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = signatureUtil.createApproveSignature(authToken, timestamp);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("mid", props.getMid());
        body.add("authToken", authToken);
        body.add("price", price);
        body.add("timestamp", timestamp);
        body.add("signature", signature);
        body.add("charset", "UTF-8");
        body.add("format", "JSON");

        // Content-Type: application/x-www-form-urlencoded 지정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // RestTemplate로 POST 수행
        RestTemplate restTemplate = new RestTemplate();

        try {
            // 승인 API 호출
            String raw = restTemplate.postForObject(
                    authUrl,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (raw == null || raw.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "웰컴 승인 응답이 비어있습니다.");
            }

            return raw;

        } catch (RestClientException e) {
            // 통신/타임아웃/4xx/5xx 등
            log.warn("웰컴 승인 통신 실패: authUrl={}, msg={}", authUrl, e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENTS_API_COMMUNICATION_ERROR, "웰컴 결제 서버와 통신에 실패했습니다.", e);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PAYMENTS_API_COMMUNICATION_ERROR, "웰컴 승인 호출 중 알 수 없는 오류가 발생했습니다.", e);
        }
    }

    public String netCancelRaw(String netCancelUrl, String authToken, String price) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = signatureUtil.createApproveSignature(authToken, timestamp);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("mid", props.getMid());
        body.add("authToken", authToken);
        body.add("price", price);
        body.add("timestamp", timestamp);
        body.add("signature", signature);
        body.add("charset", "UTF-8");
        body.add("format", "JSON");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        RestTemplate restTemplate = new RestTemplate();

        try {
            String raw = restTemplate.postForObject(
                    netCancelUrl,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            return (raw == null) ? "" : raw;
        } catch (Exception e) {
            log.warn("웰컴 망취소 통신 실패: url={}, msg={}", netCancelUrl, e.getMessage());
            return "";
        }
    }

}
