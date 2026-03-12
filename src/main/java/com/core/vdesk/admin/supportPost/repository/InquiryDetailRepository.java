package com.core.vdesk.admin.supportPost.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.core.vdesk.admin.supportPost.entity.InquiryDetails;

public interface InquiryDetailRepository extends JpaRepository<InquiryDetails, Long> {
}
