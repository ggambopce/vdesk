package com.core.vdesk.admin.supportPost.dto;

import com.core.vdesk.admin.supportPost.entity.Visibility;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportPostUpdateRequestDto {
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private Visibility visibility;
    private Boolean pinned;
}

