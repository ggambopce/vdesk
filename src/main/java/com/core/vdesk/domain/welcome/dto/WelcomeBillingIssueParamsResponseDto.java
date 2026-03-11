package com.core.vdesk.domain.welcome.dto;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WelcomeBillingIssueParamsResponseDto {
    private String actionUrl;
    private Map<String, String> fields;
}
