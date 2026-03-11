package com.core.vdesk.domain.users.dto;

import java.time.Instant;

import com.core.vdesk.domain.users.Users;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeProfileResponseDto {
    private String email;
    private String loginType;
    private String userName;
    private Instant createdAt;
    private Instant firstJoinedAt;


    public static MeProfileResponseDto of(Users user) {

  


        return MeProfileResponseDto.builder()
                .email(user.getEmail())
                .loginType(user.getLoginType())
                .createdAt(user.getCreatedAt())
                .userName(user.getUserName())
                .build();
    }
}
