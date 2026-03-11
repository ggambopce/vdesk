package com.core.vdesk.domain.devices;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.domain.devices.dto.DeviceListResponseDto;
import com.core.vdesk.domain.devices.dto.DeviceResponseDto;
import com.core.vdesk.domain.devices.dto.LinkDeviceRequestDto;
import com.core.vdesk.domain.sessions.RemoteSessionRepository;
import com.core.vdesk.domain.sessions.SessionStatus;
import com.core.vdesk.domain.users.Users;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDeviceService {

    private static final int MAX_DEVICES = 20;

    private final DeviceRepository deviceRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RemoteSessionRepository remoteSessionRepository;

    /**
     * 접속키(deviceKey)로 기기를 내 계정에 연결
     */
    @Transactional
    public DeviceResponseDto link(Users user, LinkDeviceRequestDto req) {
        Device device = deviceRepository.findByDeviceKey(req.getDeviceKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 기기를 찾을 수 없습니다."));

        if (userDeviceRepository.existsByUserAndDevice(user, device)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 내 계정에 연결된 기기입니다.");
        }

        List<UserDevice> myDevices = userDeviceRepository.findByUser(user);
        if (myDevices.size() >= MAX_DEVICES) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "최대 기기 수(" + MAX_DEVICES + "대)를 초과했습니다.");
        }

        UserDevice ud = new UserDevice();
        ud.setUser(user);
        ud.setDevice(device);
        userDeviceRepository.save(ud);

        return DeviceResponseDto.of(ud, resolveSessionStatus(device));
    }

    /**
     * 기기 단건 조회
     */
    @Transactional(readOnly = true)
    public DeviceResponseDto getDevice(Users user, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "기기를 찾을 수 없습니다."));

        UserDevice ud = userDeviceRepository.findByUserAndDevice(user, device)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "해당 기기에 접근 권한이 없습니다."));

        return DeviceResponseDto.of(ud, resolveSessionStatus(device));
    }

    /**
     * 내 기기 목록 조회
     */
    @Transactional(readOnly = true)
    public DeviceListResponseDto getDeviceList(Users user) {
        List<UserDevice> myDevices = userDeviceRepository.findByUser(user);
        List<DeviceResponseDto> items = myDevices.stream()
                .map(ud -> DeviceResponseDto.of(ud, resolveSessionStatus(ud.getDevice())))
                .toList();

        return DeviceListResponseDto.builder()
                .items(items)
                .maxDevices(MAX_DEVICES)
                .currentDevices(items.size())
                .build();
    }

    /**
     * 기기 연결 해제 (UserDevice 삭제)
     */
    @Transactional
    public void deleteDevice(Users user, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "기기를 찾을 수 없습니다."));

        UserDevice ud = userDeviceRepository.findByUserAndDevice(user, device)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "해당 기기에 접근 권한이 없습니다."));

        userDeviceRepository.delete(ud);
    }

    private SessionStatus resolveSessionStatus(Device device) {
        return remoteSessionRepository
                .findTopByDeviceAndStatusOrderByCreatedAtDesc(device, SessionStatus.RUNNING)
                .map(s -> SessionStatus.RUNNING)
                .orElse(null);
    }
}
