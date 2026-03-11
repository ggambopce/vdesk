package com.core.vdesk.domain.devices;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.devices.dto.HeartbeatRequestDto;
import com.core.vdesk.domain.devices.dto.HeartbeatResponseDto;
import com.core.vdesk.domain.devices.dto.HostRegisterRequestDto;
import com.core.vdesk.domain.devices.dto.HostRegisterResponseDto;
import com.core.vdesk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class HostController {

    private final HostService hostService;

    /**
     * 호스트 등록
     * POST /api/host/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<HostRegisterResponseDto>> register(
            @RequestBody @Valid HostRegisterRequestDto req) {
        HostRegisterResponseDto result = hostService.register(req);
        return ResponseEntity.ok(ApiResponse.ok("호스트 등록 성공", result));
    }

    /**
     * 호스트 하트비트
     * POST /api/host/heartbeat
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<HeartbeatResponseDto>> heartbeat(
            @RequestBody @Valid HeartbeatRequestDto req) {
        HeartbeatResponseDto result = hostService.heartbeat(req);
        return ResponseEntity.ok(ApiResponse.ok("호스트 상태 갱신 성공", result));
    }
}
