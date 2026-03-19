package com.core.vdesk.domain.sessions.dto;

import com.core.vdesk.domain.sessions.RemoteSession;
import com.core.vdesk.domain.sessions.SessionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RelayInfoResponseDto {

    private String relayIp;
    private int relayPort;
    private String sessionKey;
    private SessionStatus status;

    public static RelayInfoResponseDto of(RemoteSession session) {
        return RelayInfoResponseDto.builder()
                .relayIp(session.getDevice().getRelayIp())
                .relayPort(20020)
                .sessionKey(session.getSessionKey())
                .status(session.getStatus())
                .build();
    }
}
