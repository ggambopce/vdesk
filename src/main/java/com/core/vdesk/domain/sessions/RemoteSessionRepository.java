package com.core.vdesk.domain.sessions;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.core.vdesk.domain.devices.Device;
import com.core.vdesk.domain.users.Users;

public interface RemoteSessionRepository extends JpaRepository<RemoteSession, Long> {
    Optional<RemoteSession> findBySessionToken(String sessionToken);
    Optional<RemoteSession> findTopByUserAndDeviceAndStatusOrderByCreatedAtDesc(Users user, Device device, SessionStatus status);
}
