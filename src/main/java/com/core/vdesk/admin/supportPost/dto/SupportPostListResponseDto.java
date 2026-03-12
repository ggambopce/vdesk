package com.core.vdesk.admin.supportPost.dto;

import java.util.List;

public record SupportPostListResponseDto(
    int page,
    int size,
    long totalItems,
    int totalPages,
    List<SupportPostListItemResponseDto> items
) {}
