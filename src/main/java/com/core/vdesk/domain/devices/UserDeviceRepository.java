package com.core.vdesk.domain.devices;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.core.vdesk.domain.users.Users;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUser(Users user);
    Optional<UserDevice> findByUserAndDevice(Users user, Device device);
    boolean existsByUserAndDevice(Users user, Device device);
}
