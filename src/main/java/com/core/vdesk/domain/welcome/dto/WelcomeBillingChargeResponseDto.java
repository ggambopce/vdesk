package com.core.vdesk.domain.welcome.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WelcomeBillingChargeResponseDto {
    private String requestUrl;
    private String requestBody;
    private String signatureSource;
    private String rawResponse;
}
