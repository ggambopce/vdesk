package com.core.vdesk.admin.supportPost.dto;

import java.time.Instant;

import com.core.vdesk.admin.supportPost.entity.BoardType;
import com.core.vdesk.admin.supportPost.entity.PostStatus;
import com.core.vdesk.admin.supportPost.entity.SupportPosts;
import com.core.vdesk.admin.supportPost.entity.Visibility;

public record SupportPostListItemResponseDto(
    Long postId,
    BoardType type,
    String title,
    String authorName,
    Visibility visibility,
    PostStatus status,
    boolean pinned,
    long viewCount,
    Instant createdAt
) {
public static SupportPostListItemResponseDto from(SupportPosts p) {
    // author는 LAZY지만, 서비스 트랜잭션 안에서 DTO로 뽑으면 안전(대부분)
    String authorName = (p.getAuthor() != null) ? p.getAuthor().getUserName() : "unknown";
    return new SupportPostListItemResponseDto(
            p.getId(),
            p.getType(),
            p.getTitle(),
            authorName,
            p.getVisibility(),
            p.getStatus(),
            p.isPinned(),
            p.getViewCount(),
            p.getCreatedAt()
    );
}
}

