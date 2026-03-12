package com.core.vdesk.domain.sessions;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.core.vdesk.domain.devices.Device;
import com.core.vdesk.domain.users.Users;

public interface RemoteSessionRepository extends JpaRepository<RemoteSession, Long> {
    Optional<RemoteSession> findBySessionKey(String sessionKey);
    Optional<RemoteSession> findTopByDeviceAndStatusOrderByCreatedAtDesc(Device device, SessionStatus status);
    Optional<RemoteSession> findTopByDevice_DeviceKeyAndStatusOrderByCreatedAtDesc(String deviceKey, SessionStatus status);
    List<RemoteSession> findByUserOrderByCreatedAtDesc(Users user);
    Optional<RemoteSession> findTopByDeviceAndUserOrderByCreatedAtDesc(Device device, Users user);
}
