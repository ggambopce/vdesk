package com.core.vdesk.domain.devices.dto;

import java.time.Instant;

import com.core.vdesk.domain.devices.Device;
import com.core.vdesk.domain.devices.DeviceStatus;
import com.core.vdesk.domain.devices.UserDevice;
import com.core.vdesk.domain.sessions.SessionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceResponseDto {
    private Long deviceId;
    private String deviceKey;
    private String deviceName;      // aliasName 우선, 없으면 hostName
    private String platform;        // osType
    private DeviceStatus hostStatus;
    private SessionStatus sessionStatus;
    private Instant createdAt;
    private Instant updatedAt;

    public static DeviceResponseDto of(UserDevice ud, SessionStatus sessionStatus) {
        Device d = ud.getDevice();
        String name = (ud.getAliasName() != null && !ud.getAliasName().isBlank())
                ? ud.getAliasName() : d.getHostName();
        return DeviceResponseDto.builder()
                .deviceId(d.getId())
                .deviceKey(d.getDeviceKey())
                .deviceName(name)
                .platform(d.getOsType())
                .hostStatus(d.getHostStatus())
                .sessionStatus(sessionStatus)
                .createdAt(ud.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
