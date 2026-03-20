package com.core.vdesk.domain.sessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.sessions.dto.AgentActivateRequestDto;
import com.core.vdesk.domain.sessions.dto.AgentPollRequestDto;
import com.core.vdesk.domain.sessions.dto.AgentSessionPollResponseDto;
import com.core.vdesk.domain.sessions.dto.RelayInfoResponseDto;
import com.core.vdesk.domain.sessions.dto.SessionResponseDto;
import com.core.vdesk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agent/sessions")
@RequiredArgsConstructor
public class AgentSessionController {

    private static final Logger log = LoggerFactory.getLogger(AgentSessionController.class);

    private final RemoteSessionService remoteSessionService;

    /**
     * VM이 대기 세션 조회 (deviceKey 인증)
     */
    @PostMapping("/poll")
    public ResponseEntity<ApiResponse<AgentSessionPollResponseDto>> poll(
            @Valid @RequestBody AgentPollRequestDto req) {
        log.info("[POLL] deviceKey={}", req.getDeviceKey());
        AgentSessionPollResponseDto result = remoteSessionService.pollForSession(req.getDeviceKey());
        log.info("[POLL] 결과 hasPendingSession={}", result.isHasPendingSession());
        return ResponseEntity.ok(ApiResponse.ok("폴링 완료", result));
    }

    /**
     * VM이 세션 수락 (deviceKey + sessionKey 검증, RUNNING 전환)
     */
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<SessionResponseDto>> activate(
            @Valid @RequestBody AgentActivateRequestDto req) {
        SessionResponseDto result = remoteSessionService.activateSessionByAgent(
                req.getDeviceKey(), req.getSessionKey());
        return ResponseEntity.ok(ApiResponse.ok("세션 활성화 완료", result));
    }

    /**
     * 뷰어가 sessionKey로 relayIp 조회 (RUNNING 상태만 허용)
     */
    @GetMapping("/relay")
    public ResponseEntity<ApiResponse<RelayInfoResponseDto>> relay(
            @RequestParam String sessionKey) {
        RelayInfoResponseDto result = remoteSessionService.getRelayInfo(sessionKey);
        return ResponseEntity.ok(ApiResponse.ok("릴레이 정보 조회 완료", result));
    }

    /**
     * VM이 세션 종료
     */
    @PostMapping("/end")
    public ResponseEntity<ApiResponse<SessionResponseDto>> end(
            @Valid @RequestBody AgentActivateRequestDto req) {
        SessionResponseDto result = remoteSessionService.endSessionByAgent(
                req.getDeviceKey(), req.getSessionKey());
        return ResponseEntity.ok(ApiResponse.ok("세션 종료 완료", result));
    }
}
