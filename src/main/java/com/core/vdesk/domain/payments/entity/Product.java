package com.core.vdesk.domain.payments.entity;

import com.core.vdesk.domain.payments.enums.ProductCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 64)
    private ProductCode productCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Long monthlyAmount;

    @Column(nullable = false, length = 10)
    private String currency = "KRW";

    @Column(length = 100)
    private String cpu;

    @Column(length = 50)
    private String ram;

    @Column(length = 50)
    private String ssd;

    @Column(length = 100)
    private String gpu;

    @Column(length = 100)
    private String os;

    @Column(name = "public_ip", length = 100)
    private String publicIp;
}
