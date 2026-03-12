package com.core.vdesk.domain.devices.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VersionCheckResponseDto {
    private boolean updateRequired;
    private String platform;
    private String latestVersion;
    private String downloadPath;
}
