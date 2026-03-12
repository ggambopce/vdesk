package com.core.vdesk.admin.supportPost.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "partnership_details")
@Getter
@Setter
public class PartnershipDetails {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private SupportPosts post;

    @Column(length = 200)
    private String companyName;

    @Column(length = 100)
    private String contactName;

    @Column(length = 200)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String proposal;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private Instant answeredAt;
}
