package com.core.vdesk.domain.sessions.dto;

import lombok.Data;

@Data
public class CurrentSessionStatusRequestDto {
    private String sessionKey;
    private String deviceKey;
}
