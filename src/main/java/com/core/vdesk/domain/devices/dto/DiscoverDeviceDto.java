package com.core.vdesk.domain.devices.dto;

import java.time.Instant;

import com.core.vdesk.domain.devices.Device;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscoverDeviceDto {

    private Long deviceId;
    private String deviceKey;
    private String hostName;
    private String osType;
    private Instant lastSeenAt;

    public static DiscoverDeviceDto of(Device device) {
        return DiscoverDeviceDto.builder()
                .deviceId(device.getId())
                .deviceKey(device.getDeviceKey())
                .hostName(device.getHostName())
                .osType(device.getOsType())
                .lastSeenAt(device.getLastSeenAt())
                .build();
    }
}
