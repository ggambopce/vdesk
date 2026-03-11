package com.core.vdesk.domain.welcome.dto;

import lombok.Data;

@Data
public class WelcomeApproveResponseDto {
    private String tid;
    private String resultCode;
    private String resultMsg;
    private String TotPrice;
    private String MOID;
    private String payMethod;
    private String applNum;
    private String applDate;
    private String applTime;
    private String buyerEmail;
    private String buyerTel;
    private String buyerName;

    // 카드일 때 추가 필드
    private String CARD_Num;
    private String CARD_Interest;
    private String CARD_Quota;
    private String CARD_Code;
    private String CARD_PRTC_CODE;
    private String CARD_BankCode;
    private String CARD_SrcCode;
    private String CARD_Point;
    private String currency;
    private String OrgPrice;
}
