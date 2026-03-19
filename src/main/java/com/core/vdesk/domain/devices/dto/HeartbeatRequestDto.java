package com.core.vdesk.domain.devices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HeartbeatRequestDto {

    @NotBlank
    private String deviceKey;

    @Size(max = 64)
    private String relayIp;
}
