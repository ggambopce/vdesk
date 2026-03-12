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
@Table(name = "inquiry_details")
@Getter
@Setter
public class InquiryDetails {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private SupportPosts post;

    @Column(length = 200)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private Instant answeredAt;
}
