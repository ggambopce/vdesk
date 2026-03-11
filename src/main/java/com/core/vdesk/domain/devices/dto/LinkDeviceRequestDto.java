package com.core.vdesk.domain.devices.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkDeviceRequestDto {

    @NotBlank
    private String deviceKey;
}
