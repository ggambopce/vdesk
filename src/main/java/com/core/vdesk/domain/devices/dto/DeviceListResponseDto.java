package com.core.vdesk.domain.devices.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceListResponseDto {
    private List<DeviceResponseDto> items;
    private int maxDevices;
    private int currentDevices;
}
