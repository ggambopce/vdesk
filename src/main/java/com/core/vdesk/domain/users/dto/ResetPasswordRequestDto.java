package com.core.vdesk.domain.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDto {

    @NotBlank
    @Size(max=120)
    @Email
    private String email;
    /**
     * 비밀번호 정규식
     * 영문자(대문자 또는 소문자)와 숫자가 최소 1개 이상씩 포함
     * 특수문자 !@#$%^&*()_+-={}[]:;"'<>,.?/ 허용
     * 최소 8자 이상
     */
    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]{8,}$")
    private String password;

    @NotBlank
    private String confirmPassword;
}

