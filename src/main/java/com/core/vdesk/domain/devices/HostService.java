package com.core.vdesk.domain.devices;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.domain.devices.dto.HeartbeatRequestDto;
import com.core.vdesk.domain.devices.dto.HeartbeatResponseDto;
import com.core.vdesk.domain.devices.dto.HostRegisterRequestDto;
import com.core.vdesk.domain.devices.dto.HostRegisterResponseDto;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HostService {

    private final DeviceRepository deviceRepository;

    private static final String KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int KEY_SUFFIX_LENGTH = 19;

    /**
     * 호스트 등록 또는 재등록.
     * - localBox가 이미 존재하면 정보 갱신 + ONLINE 처리
     * - 신규이면 deviceKey 발급 후 등록
     */
    @Transactional
    public HostRegisterResponseDto register(HostRegisterRequestDto req) {
        Device device = deviceRepository.findByLocalBox(req.getLocalBox())
                .orElseGet(Device::new);

        if (device.getId() == null) {
            device.setLocalBox(req.getLocalBox());
            device.setDeviceKey(generateUniqueDeviceKey());
        }

        device.setHostName(req.getHostName());
        device.setOsType(req.getOsType());
        device.setAppVersion(req.getAppVersion());
        device.setRelayIp(req.getRelayIp());
        device.setHostStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(Instant.now());

        deviceRepository.save(device);
        return HostRegisterResponseDto.of(device);
    }

    /**
     * 호스트 하트비트 수신 - ONLINE 상태 갱신
     */
    @Transactional
    public HeartbeatResponseDto heartbeat(HeartbeatRequestDto req) {
        Device device = deviceRepository.findByDeviceKey(req.getDeviceKey())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "등록된 호스트를 찾을 수 없습니다."));

        if (req.getRelayIp() != null && !req.getRelayIp().isBlank()) {
            device.setRelayIp(req.getRelayIp());
        }
        device.setHostStatus(DeviceStatus.ONLINE);
        device.setLastSeenAt(Instant.now());
        deviceRepository.save(device);

        return HeartbeatResponseDto.of(device);
    }

    private String generateUniqueDeviceKey() {
        String key;
        do {
            key = "BX_" + randomSuffix();
        } while (deviceRepository.findByDeviceKey(key).isPresent());
        return key;
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(KEY_SUFFIX_LENGTH);
        for (int i = 0; i < KEY_SUFFIX_LENGTH; i++) {
            sb.append(KEY_CHARS.charAt(ThreadLocalRandom.current().nextInt(KEY_CHARS.length())));
        }
        return sb.toString();
    }
}
