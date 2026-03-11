package com.core.vdesk.domain.welcome.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "welcome-billing")
public class WelcomeBillingProps {
    private String mid;

    private String nextUrl;        // 결제 최종 승인 리다이렉트

    // Mobile Web 결제창
    private String mobileHost;     // 예: tmobile.paywelcome.co.kr or mobile.paywelcome.co.kr
    private String mobileCardPath; // 예: /smart/wcard/

    // signKey 원문(문서의 signKey)
    private String signKey;

    // PayAPI
    private String payApiBaseUrl;  // 예: https://payapi.paywelcome.co.kr
     // (PayAPI 문서에 맞게)
}
