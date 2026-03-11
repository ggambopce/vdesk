package com.core.vdesk.global.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // source 먼저 생성
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 1) PG return 전용 (쿠키 필요 없음)
        CorsConfiguration pgReturnConfig = new CorsConfiguration();
        pgReturnConfig.setAllowCredentials(false);
        pgReturnConfig.setAllowedOriginPatterns(List.of("*"));
        pgReturnConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        pgReturnConfig.setAllowedHeaders(List.of("*"));

        // return 경로만 예외로 등록
        source.registerCorsConfiguration("/api/payments/welcome/billing/return", pgReturnConfig);
        source.registerCorsConfiguration("/api/payments/webhooks/welcome/billing/return", pgReturnConfig);
        source.registerCorsConfiguration("/api/payments/webhooks/welcome/return/**", pgReturnConfig);
        source.registerCorsConfiguration("/api/payments/webhooks/welcome/close/**", pgReturnConfig);
        source.registerCorsConfiguration("/api/payments/welcome/billing/billkey/**", pgReturnConfig);

        CorsConfiguration config = new CorsConfiguration();
        /**
         * credentials 허용 여부 설정.
         * - true로 설정하면 브라우저는 쿠키, Authorization 헤더 등
         *   인증 정보(자격 증명)를 포함한 요청을 전송할 수 있다.
         * - 단, 이 경우 allowedOrigins에 와일드카드("*")를 사용할 수 없고, 구체적인 Origin만 허용해야 한다.
         *   (브라우저 CORS 규격 상 제약 사항)
         */
        config.setAllowCredentials(true);
        // 허용할 프론트엔드 주소 목록
        config.setAllowedOrigins(List.of(
                "http://localhost:5174",
                "http://localhost:8080",
                "http://218.38.136.81:8080",
                "https://multiloader.duckdns.org",
                "https://sandboxie.co.kr",
                "https://vivisectible-quinton-uncondensably.ngrok-free.dev"
        ));
        // 허용할 HTTP 메서드 목록
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        // 허용할 헤더 목록
        config.setAllowedHeaders(List.of("*"));
        /**
         * 브라우저가 JavaScript 코드에서 접근할 수 있도록 노출할 응답 헤더 목록.
         * - 여기서는 서버에서 내려주는 "Set-Cookie" 헤더를
         *   프론트엔드에서 참고할 수 있도록 노출한다.
         * - 예: Axios 응답 인터셉터에서 Set-Cookie 헤더를 로깅하거나,디버깅용으로 확인할 때 사용할 수 있다.
         */
        config.setExposedHeaders(List.of("Set-Cookie"));

        /**
         * 위에서 정의한 CORS 정책을 특정 URL 패턴에 매핑한다.
         * - "/api/**" 로 설정했기 때문에, 애플리케이션의 /api/** 엔드포인트에 동일한 CORS 설정이 적용된다.
         */
        source.registerCorsConfiguration("/api/**", config);


        return source;
    }
}