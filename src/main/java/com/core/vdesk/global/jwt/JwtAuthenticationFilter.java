package com.core.vdesk.global.jwt;

import java.util.Arrays;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.core.vdesk.domain.users.UserRepository;
import com.core.vdesk.global.oauth2.PrincipalDetails;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwt;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 쿠키에서 AT로 시작하는 액세스 토큰 추출
        String token = readCookie(req, "AT");

        if (StringUtils.hasText(token)) {
            try {
                // validateToken / isExpired 내부에서 예외가 나더라도 여기서 잡아준다
                if (jwt.validateToken(token) && !jwt.isExpired(token)) {

                    TokenMeta meta = jwt.parse(token);

                    if (meta.getType() == TokenType.ACCESS
                            && SecurityContextHolder.getContext().getAuthentication() == null) {

                        userRepository.findByEmail(meta.getEmail()).ifPresent(user -> {
                            var principal = new PrincipalDetails(user);
                            var auth = new UsernamePasswordAuthenticationToken(
                                    principal, null, principal.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            log.debug("AT OK email={}", meta.getEmail());
                        });
                    }

                } else {
                    // 토큰이 비정상이면 그냥 인증정보만 비우고 끝
                    clearContextIfNeeded();
                    log.debug("Invalid or expired JWT, treat as anonymous");
                    // 필요하면 여기서 쿠키 삭제도 가능:
                    // clearAuthCookie(res);
                }
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
                // 서명 오류, 파싱 오류 등 모든 JWT 관련 예외는 여기로
                log.debug("JWT parse/verify failed, treat as anonymous. token={}", token, e);
                clearContextIfNeeded();
                // 필요하면 쿠키 삭제:
                // clearAuthCookie(res);
            }
        } else {
            // 쿠키에 토큰 자체가 없으면 그냥 익명
            clearContextIfNeeded();
        }

        // 이 필터는 "인증만 시도"하고 항상 체인을 흘려보낸다
        chain.doFilter(req, res);
    }

    private void clearContextIfNeeded() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 액세스 토큰 추출 로직
     *
     * @param req
     * @param name
     * @return
     */
    private String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/oauth2/")
                || p.startsWith("/login/oauth2/")
                || p.startsWith("/api/oauth/apple/")
                || p.equals("/api/auth/refresh")      // 리프레시는 RT로만 처리
                || p.startsWith("/api/landing/")
                || p.startsWith("/error")
                || p.startsWith("/css/")
                || p.startsWith("/js/")
                || p.startsWith("/images/")
                || p.startsWith("/favicon/");
    }
}
