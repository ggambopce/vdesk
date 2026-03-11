package com.core.vdesk.domain.sessions;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.domain.devices.Device;
import com.core.vdesk.domain.devices.DeviceRepository;
import com.core.vdesk.domain.devices.DeviceStatus;
import com.core.vdesk.domain.devices.UserDeviceRepository;
import com.core.vdesk.domain.sessions.dto.SessionResponseDto;
import com.core.vdesk.domain.sessions.dto.StartSessionRequestDto;
import com.core.vdesk.domain.users.Users;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoteSessionService {

    private final RemoteSessionRepository remoteSessionRepository;
    private final DeviceRepository deviceRepository;
    private final UserDeviceRepository userDeviceRepository;

    /**
     * 원격 세션 시작
     * - 소유권 확인
     * - 기기 ONLINE 확인
     * - 세션 생성 및 sessionKey 발급
     */
    @Transactional
    public SessionResponseDto startSession(Users user, StartSessionRequestDto req) {
        Device device = deviceRepository.findById(req.getDeviceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "기기를 찾을 수 없습니다."));

        if (!userDeviceRepository.existsByUserAndDevice(user, device)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 기기에 접근 권한이 없습니다.");
        }

        if (device.getHostStatus() != DeviceStatus.ONLINE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "기기가 오프라인 상태입니다.");
        }

        RemoteSession session = new RemoteSession();
        session.setUser(user);
        session.setDevice(device);
        session.setSessionKey(UUID.randomUUID().toString());
        session.setStatus(SessionStatus.RUNNING);
        session.setStartedAt(Instant.now());

        remoteSessionRepository.save(session);
        return SessionResponseDto.of(session);
    }

    /**
     * 세션 단건 조회
     */
    @Transactional(readOnly = true)
    public SessionResponseDto getSession(Users user, Long sessionId) {
        RemoteSession session = remoteSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));

        if (!session.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 세션에 접근 권한이 없습니다.");
        }

        return SessionResponseDto.of(session);
    }

    /**
     * 세션 종료
     */
    @Transactional
    public SessionResponseDto endSession(Users user, Long sessionId) {
        RemoteSession session = remoteSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));

        if (!session.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 세션에 접근 권한이 없습니다.");
        }

        session.setStatus(SessionStatus.ENDED);
        session.setEndedAt(Instant.now());
        remoteSessionRepository.save(session);

        return SessionResponseDto.of(session);
    }
}
