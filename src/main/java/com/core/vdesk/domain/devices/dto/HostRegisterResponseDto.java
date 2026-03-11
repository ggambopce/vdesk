package com.core.vdesk.domain.devices.dto;

import com.core.vdesk.domain.devices.Device;
import com.core.vdesk.domain.devices.DeviceStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HostRegisterResponseDto {
    private Long hostId;
    private String deviceKey;
    private DeviceStatus hostStatus;

    public static HostRegisterResponseDto of(Device device) {
        return HostRegisterResponseDto.builder()
                .hostId(device.getId())
                .deviceKey(device.getDeviceKey())
                .hostStatus(device.getHostStatus())
                .build();
    }
}
