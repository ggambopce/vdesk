package com.core.vdesk.domain.welcome.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WelcomeMobileResultDto {
    private String P_STATUS;
    private String P_RMESG1;
    private String P_TID;
    private String P_TYPE;

    private String P_OID;
    private String P_MID;
    private String P_AMT;
    private String P_UNAME;

    // 빌키
    private String P_CARD_BILLKEY;
    private String P_FN_CD1;
    private String P_CARD_NUMHASH;
}
