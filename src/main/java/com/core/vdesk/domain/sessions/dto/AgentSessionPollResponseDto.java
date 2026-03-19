package com.core.vdesk.domain.sessions.dto;

import com.core.vdesk.domain.sessions.RemoteSession;
import com.core.vdesk.domain.sessions.SessionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentSessionPollResponseDto {

    private boolean hasPendingSession;
    private Long sessionId;
    private String sessionKey;
    private SessionStatus status;

    public static AgentSessionPollResponseDto none() {
        return AgentSessionPollResponseDto.builder()
                .hasPendingSession(false)
                .build();
    }

    public static AgentSessionPollResponseDto of(RemoteSession s) {
        return AgentSessionPollResponseDto.builder()
                .hasPendingSession(true)
                .sessionId(s.getId())
                .sessionKey(s.getSessionKey())
                .status(s.getStatus())
                .build();
    }
}
