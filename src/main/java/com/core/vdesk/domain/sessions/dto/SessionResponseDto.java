package com.core.vdesk.domain.sessions.dto;

import java.time.Instant;

import com.core.vdesk.domain.sessions.RemoteSession;
import com.core.vdesk.domain.sessions.SessionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponseDto {
    private Long sessionId;
    private String sessionKey;
    private SessionStatus status;
    private Long deviceId;
    private String deviceKey;
    private String deviceName;
    private Instant startedAt;
    private Instant endedAt;
    private String viewerUrl;
    private String relayIp;
    private int relayPort;

    public static SessionResponseDto of(RemoteSession session) {
        String relayIp = session.getDevice().getRelayIp();
        int relayPort = 20020;
        String viewerUrl = "/remote/viewer?relayIp=" + relayIp + "&port=" + relayPort;
        return SessionResponseDto.builder()
                .sessionId(session.getId())
                .sessionKey(session.getSessionKey())
                .status(session.getStatus())
                .deviceId(session.getDevice().getId())
                .deviceKey(session.getDevice().getDeviceKey())
                .deviceName(session.getDevice().getHostName())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .relayIp(relayIp)
                .relayPort(relayPort)
                .viewerUrl(viewerUrl)
                .build();
    }
}
