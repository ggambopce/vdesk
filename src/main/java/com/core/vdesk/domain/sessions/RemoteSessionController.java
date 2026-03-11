package com.core.vdesk.domain.sessions;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.sessions.dto.SessionResponseDto;
import com.core.vdesk.domain.sessions.dto.StartSessionRequestDto;
import com.core.vdesk.global.oauth2.PrincipalDetails;
import com.core.vdesk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/remote")
@RequiredArgsConstructor
public class RemoteSessionController {

    private final RemoteSessionService remoteSessionService;

    /**
     * 원격 세션 시작
     * POST /api/remote/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<SessionResponseDto>> startSession(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody @Valid StartSessionRequestDto req) {
        SessionResponseDto result = remoteSessionService.startSession(principal.getUser(), req);
        return ResponseEntity.ok(ApiResponse.ok("원격 세션 시작 성공", result));
    }

    /**
     * 세션 단건 조회
     * GET /api/remote/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponseDto>> getSession(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long sessionId) {
        SessionResponseDto result = remoteSessionService.getSession(principal.getUser(), sessionId);
        return ResponseEntity.ok(ApiResponse.ok("세션 조회 성공", result));
    }

    /**
     * 세션 종료
     * POST /api/remote/sessions/{sessionId}/end
     */
    @PostMapping("/sessions/{sessionId}/end")
    public ResponseEntity<ApiResponse<SessionResponseDto>> endSession(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long sessionId) {
        SessionResponseDto result = remoteSessionService.endSession(principal.getUser(), sessionId);
        return ResponseEntity.ok(ApiResponse.ok("세션 종료 성공", result));
    }
}
