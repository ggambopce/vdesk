package com.core.vdesk.domain.sessions;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.sessions.dto.ActivateSessionRequestDto;
import com.core.vdesk.domain.sessions.dto.CurrentSessionStatusRequestDto;
import com.core.vdesk.domain.sessions.dto.EndSessionByKeyRequestDto;
import com.core.vdesk.domain.sessions.dto.SessionResponseDto;
import com.core.vdesk.global.oauth2.PrincipalDetails;
import com.core.vdesk.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/devices/sessions")
@RequiredArgsConstructor
public class DeviceSessionController {

    private final RemoteSessionService remoteSessionService;

    /**
     * C# 에이전트: deviceKey로 세션 활성화
     * POST /api/devices/sessions/activate
     */
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<SessionResponseDto>> activate(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody ActivateSessionRequestDto req) {
        SessionResponseDto result = remoteSessionService.activateSession(principal.getUser(), req.getDeviceKey());
        return ResponseEntity.ok(ApiResponse.ok("세션 활성화 성공", result));
    }

    /**
     * C# 에이전트: sessionKey로 세션 종료
     * POST /api/devices/sessions/end
     */
    @PostMapping("/end")
    public ResponseEntity<ApiResponse<SessionResponseDto>> end(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody EndSessionByKeyRequestDto req) {
        SessionResponseDto result = remoteSessionService.endSessionByKey(principal.getUser(), req.getSessionKey());
        return ResponseEntity.ok(ApiResponse.ok("세션 종료 성공", result));
    }

    /**
     * 현재 세션 상태 폴링
     * POST /api/devices/sessions/status/me/current
     */
    @PostMapping("/status/me/current")
    public ResponseEntity<ApiResponse<SessionResponseDto>> currentStatus(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody CurrentSessionStatusRequestDto req) {
        SessionResponseDto result = remoteSessionService.getCurrentSessionStatus(
                principal.getUser(), req.getSessionKey(), req.getDeviceKey());
        return ResponseEntity.ok(ApiResponse.ok("세션 상태 조회 성공", result));
    }

    /**
     * 내 기기별 최신 세션 상태 목록
     * GET /api/devices/sessions/status/me/list
     */
    @GetMapping("/status/me/list")
    public ResponseEntity<ApiResponse<List<SessionResponseDto>>> statusList(
            @AuthenticationPrincipal PrincipalDetails principal) {
        List<SessionResponseDto> result = remoteSessionService.getMySessionStatusList(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("세션 상태 목록 조회 성공", result));
    }

    /**
     * 내 전체 세션 기록
     * GET /api/devices/sessions/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SessionResponseDto>>> mySessions(
            @AuthenticationPrincipal PrincipalDetails principal) {
        List<SessionResponseDto> result = remoteSessionService.getMyAllSessions(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("세션 목록 조회 성공", result));
    }
}
