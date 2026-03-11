package com.core.vdesk.global.jwt;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TokenMeta {
    private String email;
    private TokenType type;
    private Date issuedAt;
    private Date expiryDate;

    public boolean isExpired() {
        return expiryDate != null && expiryDate.before(new Date());
    }
}
