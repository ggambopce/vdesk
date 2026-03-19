package com.core.vdesk.domain.devices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostRegisterRequestDto {

    @NotBlank
    @Size(max = 100)
    private String hostName;

    @NotBlank
    @Size(max = 100)
    private String localBox;

    @NotBlank
    @Size(max = 20)
    private String osType;

    @Size(max = 20)
    private String appVersion;

    @NotBlank
    @Size(max = 64)
    private String relayIp;
}
