package com.core.vdesk.global.oauth2;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.core.vdesk.domain.users.Users;

import lombok.Data;

@Data
public class PrincipalDetails implements UserDetails, OAuth2User {

    private Users user;
    private Map<String, Object> attributes;

    // 일반 로그인 사용자
    public PrincipalDetails(Users user) {
        this.user = user;
        this.attributes = null;
    }

    // 소셜 로그인 사용자
    public PrincipalDetails(Users user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    // === OAuth2User ===
    @Override
    public Map<String, Object> getAttributes() { return attributes; }

    @Override
    public String getName() {
        // 구글은 sub, 없으면 내부 PK로 통일
        if (attributes != null) {
            if (attributes.containsKey("sub")) return String.valueOf(attributes.get("sub")); // Google
            if (attributes.containsKey("id"))  return String.valueOf(attributes.get("id"));  // Kakao/Naver
        }
        return String.valueOf(user.getUserId());
    }

    // === UserDetails ===
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // "ROLE_USER,ROLE_ADMIN" → [SimpleGrantedAuthority("ROLE_USER"), ...]
        return Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    /**
     * Spring Security가 “사용자 고유 식별자”로 사용할 값.
     * 이메일을 기준으로 인증을 수행하도록 통일.
     */
    @Override
    public String getUsername() {
        // 인증의 기준 키를 하나로 통일: 이메일을 권장
        return user.getEmail();
    }

    // 가독성 사용자 고유 식별자 명시적 편의 메서드
    public String getEmail() {
        return user.getEmail();
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}