package com.core.vdesk.domain.sessions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartSessionRequestDto {

    @NotNull
    private Long deviceId;
}
