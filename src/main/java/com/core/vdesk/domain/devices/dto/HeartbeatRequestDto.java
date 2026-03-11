package com.core.vdesk.domain.devices.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeartbeatRequestDto {

    @NotBlank
    private String deviceKey;
}
