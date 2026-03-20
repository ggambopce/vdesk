package com.core.vdesk.domain.devices;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByLocalBox(String localBox);
    Optional<Device> findByDeviceKey(String deviceKey);
    boolean existsByLocalBox(String localBox);
    List<Device> findByHostStatus(DeviceStatus hostStatus);
}
