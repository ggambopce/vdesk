package com.core.vdesk.domain.sessions;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.core.vdesk.domain.devices.Device;

public interface RemoteSessionRepository extends JpaRepository<RemoteSession, Long> {
    Optional<RemoteSession> findBySessionKey(String sessionKey);
    Optional<RemoteSession> findTopByDeviceAndStatusOrderByCreatedAtDesc(Device device, SessionStatus status);
}
