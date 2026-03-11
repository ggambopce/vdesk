package com.core.vdesk.domain.welcome.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WelcomeBillingIssueParamsRequestDto {
    private String productCode;
    private Integer quantity;
}
