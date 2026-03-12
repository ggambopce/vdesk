package com.core.vdesk.domain.sessions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.domain.devices.Device;
import com.core.vdesk.domain.devices.DeviceRepository;
import com.core.vdesk.domain.devices.DeviceStatus;
import com.core.vdesk.domain.devices.UserDevice;
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

    /**
     * C# 에이전트 연동: deviceKey로 세션 활성화
     * 기존 RUNNING 세션은 KILLED 처리
     */
    @Transactional
    public SessionResponseDto activateSession(Users user, String deviceKey) {
        Device device = deviceRepository.findByDeviceKey(deviceKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "기기를 찾을 수 없습니다."));

        if (!userDeviceRepository.existsByUserAndDevice(user, device)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 기기에 접근 권한이 없습니다.");
        }

        // 기존 RUNNING 세션 KILLED 처리
        remoteSessionRepository.findTopByDevice_DeviceKeyAndStatusOrderByCreatedAtDesc(deviceKey, SessionStatus.RUNNING)
                .ifPresent(existing -> {
                    existing.setStatus(SessionStatus.KILLED);
                    existing.setEndedAt(Instant.now());
                    remoteSessionRepository.save(existing);
                });

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
     * C# 에이전트 연동: sessionKey로 세션 종료
     */
    @Transactional
    public SessionResponseDto endSessionByKey(Users user, String sessionKey) {
        RemoteSession session = remoteSessionRepository.findBySessionKey(sessionKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));

        if (!session.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 세션에 접근 권한이 없습니다.");
        }

        session.setStatus(SessionStatus.ENDED);
        session.setEndedAt(Instant.now());
        remoteSessionRepository.save(session);

        return SessionResponseDto.of(session);
    }

    /**
     * 현재 세션 상태 폴링 (sessionKey + deviceKey)
     */
    @Transactional(readOnly = true)
    public SessionResponseDto getCurrentSessionStatus(Users user, String sessionKey, String deviceKey) {
        RemoteSession session = remoteSessionRepository.findBySessionKey(sessionKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));

        if (!session.getUser().getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 세션에 접근 권한이 없습니다.");
        }

        return SessionResponseDto.of(session);
    }

    /**
     * 내 기기별 최신 세션 상태 목록
     */
    @Transactional(readOnly = true)
    public List<SessionResponseDto> getMySessionStatusList(Users user) {
        List<UserDevice> userDevices = userDeviceRepository.findByUser(user);
        return userDevices.stream()
                .map(ud -> remoteSessionRepository.findTopByDeviceAndUserOrderByCreatedAtDesc(ud.getDevice(), user))
                .filter(java.util.Optional::isPresent)
                .map(opt -> SessionResponseDto.of(opt.get()))
                .collect(Collectors.toList());
    }

    /**
     * 내 전체 세션 기록
     */
    @Transactional(readOnly = true)
    public List<SessionResponseDto> getMyAllSessions(Users user) {
        return remoteSessionRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SessionResponseDto::of)
                .collect(Collectors.toList());
    }
}
