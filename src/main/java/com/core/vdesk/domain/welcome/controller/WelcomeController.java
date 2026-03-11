package com.core.vdesk.domain.welcome.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.payments.dto.OrderCreateRequestDto;
import com.core.vdesk.domain.users.Users;
import com.core.vdesk.domain.welcome.dto.WelcomePayReadyResponseDto;
import com.core.vdesk.domain.welcome.service.WelcomeService;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.oauth2.PrincipalDetails;
import com.core.vdesk.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class WelcomeController {

    private final WelcomeService welcomeService;

    /**
     * [1단계] 웰컴 Web Standard 결제창 호출을 위한 "주문서 생성 + 결제요청 파라미터" 발급 API
     * 프론트는 이 API 응답으로 받은 값을 hidden input(form)으로 구성한 뒤,
     * INIStdPay.js의 INIStdPay.pay(formId)를 호출하여 결제창을 띄운다.
     * - 서버에서 signature/mKey를 생성해야 하는 이유:
     *   클라이언트에서 생성하면 signKey 노출 위험이 있고, 위변조 검증 신뢰성이 떨어진다.
     * - buyer 정보:
     *   웰컴 Web Standard는 buyername/buyertel/buyerEmail 필수이나 SW기본정보 사용.
     */
    @PostMapping("/welcome/orders")
    public ResponseEntity<ApiResponse<WelcomePayReadyResponseDto>> welcomeOrder(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody @Valid OrderCreateRequestDto req
    ) {
        Users user = principalDetails.getUser();

        // 필수 구매자 정보는 기본정보에 맞게 구성
        String buyerName  = user.getUserName(); // 이메일 사용
        String buyerTel   = "01010047777";   // 가상번호 지정
        String buyerEmail = user.getEmail();

        // 내부 주문 생성/저장, 웰컴 요청 파라미터 생성
        WelcomePayReadyResponseDto result =
                welcomeService.createWelcomeOrderAndParams(
                        req,
                        user.getUserId(),
                        buyerName,
                        buyerTel,
                        buyerEmail                );

        return ResponseEntity.ok(ApiResponse.ok("주문서 작성 성공", result));
    }

    /**
     * [2단계] 웰컴 Web Standard returnUrl (인증결과 수신 엔드포인트)
     * 웰컴 결제 흐름 요약:
     * - 사용자 결제창에서 인증 완료 → PG가 returnUrl로 POST 전송
     * - resultCode=0000이면 인증성공 → authUrl로 서버-서버 승인요청(POST) 수행
     * - 승인 응답에서 resultCode=0000이면 최종 결제 확정(DB 저장/플랜 활성화 등)
     * - 이 엔드포인트는 "브라우저를 통해" 호출되지만, 호출 주체는 PG이다.
     */
    @PostMapping("/webhooks/welcome/return")
    public ResponseEntity<Void> welcomeReturn(HttpServletRequest request) {

        // PG가 form POST로 전달하는 인증결과 파라미터들
        String resultCode   = request.getParameter("resultCode");
        String resultMsg    = request.getParameter("resultMsg");
        String orderNumber  = request.getParameter("orderNumber");
        String authToken    = request.getParameter("authToken");
        String authUrl      = request.getParameter("authUrl");
        String netCancelUrl = request.getParameter("netCancelUrl");

        // 기본값은 cancel 로 가정 SW 클라이언트로 리다이렉트
        String redirectUrl = "http://localhost:57423/cancel";

        try {
            // PG 인증 자체가 실패한 경우 (결제창 취소, 에러 등)
            if (!"0000".equals(resultCode)) {
                log.warn("웰컴 인증 실패: orderNumber={}, resultCode={}, resultMsg={}, netCancelUrl={}",
                        orderNumber, resultCode, resultMsg, netCancelUrl);

                // 여기서는 redirectUrl 그대로 cancel 유지
            } else {
                // 인증은 성공 → 서버 간 승인 요청
                welcomeService.confirmWelcome(orderNumber, authToken, authUrl, netCancelUrl);

                // 여기까지 왔으면 승인 성공 → success를 SW 클라이언트로 리다이렉트
                redirectUrl = "http://localhost:57423/success";
            }
        } catch (BusinessException e) {
            // 승인(api) 과정에서 터지면 실패로 간주하고 cancel 로 보냄
            log.error("웰컴 승인 처리 중 오류: orderNumber={}, message={}", orderNumber, e.getMessage(), e);
            redirectUrl = "http://localhost:57423/cancel";
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)        // 302
                .location(URI.create(redirectUrl))
                .build();
    }
}

