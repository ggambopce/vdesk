package com.core.vdesk.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.core.vdesk.global.jwt.JwtAuthenticationFilter;
import com.core.vdesk.global.oauth2.CustomOauth2UserService;
import com.core.vdesk.global.oauth2.Oauth2FailureHandler;
import com.core.vdesk.global.oauth2.Oauth2SuccessHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity          // Spring Security 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOauth2UserService customOauth2UserService;
    private final Oauth2SuccessHandler oauth2SuccessHandler;
    private final Oauth2FailureHandler oauth2FailureHandler;


    /**
     * 애플리케이션의 보안 필터 체인 정의.
     * - JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 배치하여
     *   폼 로그인 대신 토큰 기반 인증을 우선 적용한다.
     * - RequestMetaFilter는 JWT 필터 이후에 배치하여,
     *   인증 컨텍스트와 함께 요청 메타데이터(IP, User-Agent, URI)를
     *   ThreadLocal에 저장해 이후 서비스/로깅 로직에서 활용할 수 있게 한다.
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                // CSRF: REST API + JWT 환경에서는 서버 세션 상태가 없어 비활성화(폼 기반에는 필요)
                .cors(cors -> {})                           // CORS: @Bean 으로 별도 구성(허용 origin, 메서드, 헤더, 크리덴셜) 가능
                .formLogin(form -> form.disable())     // 기본 로그인 페이지 비활성화
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 생성하지 않음: JWT 기반 무상태 인증
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))) // 인증 실패(미인증 접근) 시 401 응답 반환
                .authorizeHttpRequests(auth -> auth    // 엔드포인트별 접근 규칙
                        .requestMatchers(
                                // OAuth2 진입/콜백
                                "/oauth2/authorization/**", "/login/oauth2/code/**",
                                // 랜딩 / 공개 페이지
                                "/", "/intro", "/method", "/support", "/pricing",
                                "/signup", "/login", "/logout", "/privacy", "/terms",
                                "/checkout", "/payments/**", "/billing").permitAll()
                        .requestMatchers(
                                "/api/host/register",
                                "/api/host/heartbeat"
                                ).permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/signup",
                                "/api/auth/logout",
                                "/api/auth/duplications/email",
                                "/api/auth/email/verification",
                                "/api/auth/email/verify",
                                "/api/auth/refresh",
                                "/api/auth/cookie/pickup",
                                "/api/payments/**",
                                "/api/auth/password/reset",
                                "/api/landing/download",
                                "/api/landing/feedback",
                                "/api/landing/visit",
                                "/api/devices/version/update/check",
                                "/api/devices/version/create",
                                "/api/payments/webhooks/smartro/return",
                                "/api/payments/webhooks/smartro/close",
                                // PayPal 콜백/테스트용 엔드포인트 허용
                                "/api/paypal/return",
                                "/api/paypal/cancel",
                                "/api/products/list",
                                "/api/products/purchase-options",
                                "/api/payments/webhooks/welcome/return",
                                "/api/payments/webhooks/welcome/close",
                                "/api/payments/welcome/billing/return",
                                "/api/payments/welcome/billing/billkey/success",
                                "/api/paddle/webhook",
                                "/api/paddle/subscription/webhook",
                                "/api/test/sms",
                                "/api/test/sms/basic",
                                "/api/test/sms/direct"
                                ).permitAll() // 허용
                        .requestMatchers("/", "/index.html",
                                "/check.html", "/success.html", "/fail.html", "/billing.html",
                                "/paypal-check.html", "/paypal-billing.html","/welcome-check.html","/welcome-billing.html",
                                "/paddle-check.html", "/paddle-billing.html","/smartro-test.html", "/paddle-check-test.html", "/paddle-billing-test.html",
                                "/favicon/**",
                                // Vite 기본 산출물들
                                "/assets/**",      // JS/CSS 번들
                                "/vite.svg",       // Vite 아이콘
                                "/*.css",          // /index.css 같은 루트 CSS
                                "/*.js",           // 루트 JS가 있다면
                                "/fonts/**",
                                "/img/**"
                                ).permitAll()
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/withdraw",
                                // User Device API
                                "/api/user/device/link",
                                "/api/user/device/list",
                                "/api/user/device/**",
                                // Remote Session API
                                "/api/remote/sessions",
                                "/api/remote/session/**",
                                "/api/remote/sessions/**",
                                "/api/orders/**",
                                "/api/payments/welcome/billing/billkey",
                                "/api/payments/welcome/billing/issue-params",
                                "/api/payments/welcome/billing/cancel",
                                "/api/payments/welcome/billing/resume",
                                "/api/payments/welcome/orders",
                                "/api/payments/orders",
                                "/api/payments/billing/issue",
                                "/api/payments/billing/cancel",
                                "/api/paypal/subscription/activate",
                                "/api/paypal/subscription/cancel",
                                "/api/paypal/subscription/uncancel",
                                "/api/paypal/order",
                                "/api/paypal/capture",
                                "/api/paddle/order",
                                "/api/paddle/orders/**",
                                "/api/paddle/orders/close/**",
                                "/api/paddle/subscription/checkout",
                                "/api/paddle/subscription/activate",
                                "/api/paddle/subscription/orders/**",
                                "/api/paddle/subscription/orders/close/**",
                                "/api/paddle/subscription/cancel",
                                "/api/paddle/subscription/uncancel",
                                "/api/devices/register",
                                "/api/devices/register/encrypt",
                                "/api/devices/sessions/activate",
                                "/api/devices/sessions/status/me/current",
                                "/api/devices/sessions/status/me/list",
                                "/api/devices/sessions/me",
                                "/api/devices/sessions/end",
                                "/api/stats/payments/attempt",
                                "/api/stats/payments/attempt/list",
                                "/api/stats/ads/impression",
                                "/api/stats/ads/click"
                                ).authenticated() // 사용자 인증 필수
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                )
                /**
                 * OAuth2 Login 처리
                 * - 로그인 성공 시: Oauth2SuccessHandler → JWT 발급
                 * - 로그인 실패 시: Oauth2FailureHandler → 에러 처리
                 */
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOauth2UserService))
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler(oauth2FailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // 폼 인증(UsernamePasswordAuthenticationFilter)보다 먼저 토큰으로 인증 컨텍스트를 세팅
                .logout(logout -> logout.disable());        // JWT는 서버 세션이 없어 클라이언트가 토큰 제거처리로 로그아웃을 구현
        return http.build();
    }
}