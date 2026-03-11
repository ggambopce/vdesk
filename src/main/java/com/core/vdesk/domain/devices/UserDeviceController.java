package com.core.vdesk.domain.devices;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.devices.dto.DeviceListResponseDto;
import com.core.vdesk.domain.devices.dto.DeviceResponseDto;
import com.core.vdesk.domain.devices.dto.LinkDeviceRequestDto;
import com.core.vdesk.global.oauth2.PrincipalDetails;
import com.core.vdesk.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user/device")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    /**
     * 기기 연결 (접속키 입력)
     * POST /api/user/device/link
     */
    @PostMapping("/link")
    public ResponseEntity<ApiResponse<DeviceResponseDto>> link(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody @Valid LinkDeviceRequestDto req) {
        DeviceResponseDto result = userDeviceService.link(principal.getUser(), req);
        return ResponseEntity.ok(ApiResponse.ok("기기 연결 성공", result));
    }

    /**
     * 기기 단건 조회
     * GET /api/user/device/{deviceId}
     */
    @GetMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<DeviceResponseDto>> getDevice(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long deviceId) {
        DeviceResponseDto result = userDeviceService.getDevice(principal.getUser(), deviceId);
        return ResponseEntity.ok(ApiResponse.ok("기기 조회 성공", result));
    }

    /**
     * 내 기기 목록 조회
     * GET /api/user/device/list
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<DeviceListResponseDto>> getDeviceList(
            @AuthenticationPrincipal PrincipalDetails principal) {
        DeviceListResponseDto result = userDeviceService.getDeviceList(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("기기 목록 조회 성공", result));
    }

    /**
     * 기기 연결 해제
     * DELETE /api/user/device/delete/{deviceId}
     */
    @DeleteMapping("/delete/{deviceId}")
    public ResponseEntity<ApiResponse<Void>> deleteDevice(
            @AuthenticationPrincipal PrincipalDetails principal,
            @PathVariable Long deviceId) {
        userDeviceService.deleteDevice(principal.getUser(), deviceId);
        return ResponseEntity.ok(ApiResponse.ok("기기 제거 성공", null));
    }
}
