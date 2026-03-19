package com.core.vdesk.domain.sessions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentActivateRequestDto {

    @NotBlank
    private String deviceKey;

    @NotBlank
    private String sessionKey;
}
