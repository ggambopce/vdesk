package com.core.vdesk.domain.devices;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** VM/PC 고유 식별값 (접속기가 생성) */
    @Column(nullable = false, unique = true, length = 100)
    private String machineId;

    /** 사용자에게 보여주는 접속번호 (예: A1B2-C3D4) */
    @Column(nullable = false, unique = true, length = 10)
    private String deviceCode;

    @Column(nullable = false)
    private String deviceName;

    /** WINDOWS / LINUX / MAC */
    @Column(nullable = false, length = 20)
    private String osType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Column(length = 20)
    private String appVersion;

    private Instant lastSeenAt;

    @CreationTimestamp
    private Instant createdAt;
}
