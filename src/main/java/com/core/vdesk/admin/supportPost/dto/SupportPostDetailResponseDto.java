package com.core.vdesk.admin.supportPost.dto;

import java.time.Instant;
import java.util.List;

import com.core.vdesk.admin.supportPost.entity.BoardType;
import com.core.vdesk.admin.supportPost.entity.InquiryDetails;
import com.core.vdesk.admin.supportPost.entity.PartnershipDetails;
import com.core.vdesk.admin.supportPost.entity.PostStatus;
import com.core.vdesk.admin.supportPost.entity.SupportPosts;
import com.core.vdesk.admin.supportPost.entity.Visibility;
import com.core.vdesk.domain.users.Users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SupportPostDetailResponseDto {

    private PostDto post;
    private InquiryDto inquiry;
    private PartnershipDto partnership;

    /* =======================
       Post 공통 정보
       ======================= */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PostDto {
        private Long postId;
        private BoardType type;
        private String title;
        private String content;
        private Visibility visibility;
        private boolean pinned;
        private PostStatus status;
        private long viewCount;
        private Instant createdAt;
        private Instant updatedAt;
        private AuthorDto author;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AuthorDto {
        private Long userId;
        private String userName;
    }

    /* =======================
       INQUIRY 상세
       ======================= */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InquiryDto {
        private String email;
        private String phone;
        private String category;
        private String answer;
        private Instant answeredAt;
        private List<AttachmentDto> attachments;
    }

    /* =======================
       PARTNERSHIP 상세
       ======================= */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PartnershipDto {
        private String companyName;
        private String contactName;
        private String email;
        private String phone;
        private String proposal;
        private String answer;
        private Instant answeredAt;
        private List<AttachmentDto> attachments;
    }

    /* =======================
       첨부파일
       ======================= */
    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachmentDto {
        private String name;
        private String url;
    }

    /* =======================
       정적 팩토리
       ======================= */
    public static SupportPostDetailResponseDto from(
            SupportPosts post,
            InquiryDetails inquiry,
            PartnershipDetails partnership
    ) {
        SupportPostDetailResponseDto dto = new SupportPostDetailResponseDto();

        dto.setPost(PostDto.builder()
                .postId(post.getId())
                .type(post.getType())
                .title(post.getTitle())
                .content(post.getContent())
                .visibility(post.getVisibility())
                .pinned(post.isPinned())
                .status(post.getStatus())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .author(fromAuthor(post.getAuthor()))
                .build()
        );

        if (inquiry != null) {
            dto.setInquiry(InquiryDto.builder()
                    .email(inquiry.getEmail())
                    .phone(inquiry.getPhone())
                    .category(inquiry.getCategory())
                    .answer(inquiry.getAnswer())
                    .answeredAt(inquiry.getAnsweredAt())
                    .attachments(List.of()) // 첨부파일 붙이면 교체
                    .build()
            );
        }

        if (partnership != null) {
            dto.setPartnership(PartnershipDto.builder()
                    .companyName(partnership.getCompanyName())
                    .contactName(partnership.getContactName())
                    .email(partnership.getEmail())
                    .phone(partnership.getPhone())
                    .proposal(partnership.getProposal())
                    .answer(partnership.getAnswer())
                    .answeredAt(partnership.getAnsweredAt())
                    .attachments(List.of())
                    .build()
            );
        }

        return dto;
    }

    private static AuthorDto fromAuthor(Users user) {
        return AuthorDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .build();
    }
}
