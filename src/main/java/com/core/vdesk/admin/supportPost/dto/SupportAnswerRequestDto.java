package com.core.vdesk.admin.supportPost.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportAnswerRequestDto {
    @NotBlank
    private String answer;

    private boolean notify = false;
}
