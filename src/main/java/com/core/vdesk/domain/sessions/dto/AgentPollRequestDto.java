package com.core.vdesk.domain.sessions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentPollRequestDto {

    @NotBlank
    private String deviceKey;
}
