package com.core.vdesk.domain.sessions.dto;

import lombok.Data;

@Data
public class EndSessionByKeyRequestDto {
    private String sessionKey;
}
