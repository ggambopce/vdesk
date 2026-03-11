package com.core.vdesk.domain.devices.dto;

import java.time.Instant;

import com.core.vdesk.domain.devices.Device;
import com.core.vdesk.domain.devices.DeviceStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeartbeatResponseDto {
    private DeviceStatus hostStatus;
    private Instant lastSeenAt;

    public static HeartbeatResponseDto of(Device device) {
        return HeartbeatResponseDto.builder()
                .hostStatus(device.getHostStatus())
                .lastSeenAt(device.getLastSeenAt())
                .build();
    }
}
