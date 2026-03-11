package com.core.vdesk.domain.users.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawRequestDto {
    // normal 계정만 사용, social은 비워서 보낸다.
    private String password;
}
