package com.core.vdesk.domain.devices;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    /** VM/PC 고유 식별값 (접속기가 생성, 예: box56484984) */
    @Column(nullable = false, unique = true, length = 100)
    private String localBox;

    /** 서버 발급 접속키 (예: BX_RKCAJSY5FL7KUSVDXMAA) */
    @Column(nullable = false, unique = true, length = 30)
    private String deviceKey;

    /** 호스트 프로그램이 등록한 기기 이름 */
    @Column(nullable = false)
    private String hostName;

    /** WINDOWS / LINUX / MAC */
    @Column(nullable = false, length = 20)
    private String osType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus hostStatus = DeviceStatus.OFFLINE;

    @Column(length = 20)
    private String appVersion;

    /** 호스트가 등록한 릴레이 서버 IP (뷰어가 연결할 주소) */
    @Column(length = 64)
    private String relayIp;

    private Instant lastSeenAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
