package com.core.vdesk.admin.supportPost.dto;

import com.core.vdesk.admin.supportPost.entity.BoardType;
import com.core.vdesk.admin.supportPost.entity.Visibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportPostRegisterRequestDto {
    @NotNull
    private BoardType type;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private Visibility visibility;

    private boolean pinned;

    private InquiryPayload inquiry;
    private PartnershipPayload partnership;

    @Getter @Setter
    public static class InquiryPayload {
        private String email;
        private String phone;
        private String category;
    }

    @Getter @Setter
    public static class PartnershipPayload {
        private String companyName;
        private String contactName;
        private String email;
        private String phone;
        private String proposal;
    }
}
