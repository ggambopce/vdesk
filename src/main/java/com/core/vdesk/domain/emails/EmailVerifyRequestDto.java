package com.core.vdesk.domain.emails;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerifyRequestDto {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;
}
