package com.core.vdesk.domain.devices;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByMachineId(String machineId);
    Optional<Device> findByDeviceCode(String deviceCode);
    boolean existsByMachineId(String machineId);
}
