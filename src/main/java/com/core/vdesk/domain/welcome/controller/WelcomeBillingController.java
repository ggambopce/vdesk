package com.core.vdesk.domain.welcome.controller;

import org.springframework.http.HttpHeaders;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.users.UserRepository;
import com.core.vdesk.domain.users.Users;
import com.core.vdesk.domain.welcome.dto.WelcomeBillingIssueParamsRequestDto;
import com.core.vdesk.domain.welcome.dto.WelcomeBillingIssueParamsResponseDto;
import com.core.vdesk.domain.welcome.dto.WelcomeMobileResultDto;
import com.core.vdesk.domain.welcome.service.WelcomeBillingService;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.oauth2.PrincipalDetails;
import com.core.vdesk.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/welcome/billing")
public class WelcomeBillingController {

    private final UserRepository userRepository;
    private final WelcomeBillingService welcomeBillingService;

    @PostMapping("/issue-params")
    public ResponseEntity<ApiResponse<WelcomeBillingIssueParamsResponseDto>> issueParams(
            @AuthenticationPrincipal PrincipalDetails principal,
            @RequestBody WelcomeBillingIssueParamsRequestDto req
    ) {
        Users user = userRepository.findById(principal.getUser().getUserId())
                .orElseThrow();

        WelcomeBillingIssueParamsResponseDto result =
                welcomeBillingService.issueParms(user, req.getProductCode(), req.getQuantity());

        return ResponseEntity.ok(ApiResponse.ok("주문서 작성 성공", result));
    }

    /**
     * 웰컴 모바일 결제창(P_NEXT_URL) 콜백
     * - 브라우저 컨텍스트에서 열리므로 여기서 결과 페이지로 302 리다이렉트 처리
     *
     * 성공: /billing/result?ok=1&orderId=...
     * 실패: /billing/result?ok=0&code=...&msg=...
     */
    @PostMapping(value = "/billkey/success", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> completePost(WelcomeMobileResultDto r, @RequestParam Map<String, String> raw) {
        return handleAndRedirect(r, raw);
    }

    @GetMapping("/billkey/success")
    public ResponseEntity<Void> completeGet(WelcomeMobileResultDto r, @RequestParam Map<String, String> raw) {
        return handleAndRedirect(r, raw);
    }

    private ResponseEntity<Void> handleAndRedirect(WelcomeMobileResultDto r, Map<String, String> raw) {
        // 콜백 “진입 로그”는 Service에서도 찍지만, 컨트롤러에서 1번 더 찍는다.
        log.info("웰컴 빌키 확보 성공 콜백 원문 컨트롤러 진입 rawKeys={}", raw == null ? null : raw.keySet());

        try {
             welcomeBillingService.handleBillKeyCallbackAndFirstCharge(r, raw);

            String location = "http://localhost:57423/success";
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, location)
                    .build();

        } catch (BusinessException e) {
            String location = "http://localhost:57423/cancel";
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, location)
                    .build();

        } catch (Exception e) {
            String location = "http://localhost:57423/cancel";
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, location)
                    .build();
        }
    }

    /**
     * [3] 구독 해지 API
     * 서비스 내부 구독을 중단한다.
     * - UserPlan: CANCEL_SCHEDULED 처리 + 다음 청구(nextChargeDate) 제거 + FREE 전환
     * - WelcomeBillingKey: INACTIVE 처리하여 스케줄러 자동청구 대상에서 제외
     * 참고:
     * - “스마트로 빌링키 삭제(ssbdel.do)”까지 물리적으로 삭제하려면
     *   별도 API 연동을 추가하여 함께 호출할 수 있다(정책 선택).
     */
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal PrincipalDetails principal
    ) {
        Long userId = principal.getUser().getUserId();
        // 내부 구독 해지 + 빌링키 비활성화
        welcomeBillingService.cancelBilling(userId);
        return ResponseEntity.ok(ApiResponse.ok("구독 해지 완료", null));
    }

    /**
     * 구독 재개
     */
    @PostMapping("/resume")
    public ResponseEntity<ApiResponse<Void>> resume(@AuthenticationPrincipal PrincipalDetails principal) {
        Long userId = principal.getUser().getUserId();
        welcomeBillingService.resumeBilling(userId);
        return ResponseEntity.ok(ApiResponse.ok("구독 재개 완료", null));
    }

}

