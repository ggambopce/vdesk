package com.core.vdesk.domain.welcome.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WelcomeBillingChargeRequestDto {
    private String billkey;
    private String goodsName;
    private String buyerName;
    private String buyerTel;
    private String buyerEmail;
    private String price;
    private String oid;
    private String cardQuota;
    private String quotaInterest;
}
