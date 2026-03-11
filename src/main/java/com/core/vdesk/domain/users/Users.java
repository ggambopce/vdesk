package com.core.vdesk.domain.users;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(indexes = {
        @Index(name="idx_users_email_hash", columnList="email_hash")
})
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column
    private String userName;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String roles = "ROLE_USER";

    @Column(nullable = false, length = 20)
    private String loginType;       // normal, google

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    private Instant createdAt;


}
