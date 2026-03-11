package com.core.vdesk.domain.welcome.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.domain.payments.dto.OrderCreateRequestDto;
import com.core.vdesk.domain.payments.entity.Orders;
import com.core.vdesk.domain.payments.entity.Payment;
import com.core.vdesk.domain.payments.entity.Product;
import com.core.vdesk.domain.payments.entity.UserPlan;
import com.core.vdesk.domain.payments.enums.BillingProvider;
import com.core.vdesk.domain.payments.enums.OrderStatus;
import com.core.vdesk.domain.payments.enums.OrderType;
import com.core.vdesk.domain.payments.enums.PayProvider;
import com.core.vdesk.domain.payments.enums.PaymentStatus;
import com.core.vdesk.domain.payments.enums.PlanType;
import com.core.vdesk.domain.payments.enums.ProductCode;
import com.core.vdesk.domain.payments.enums.TxStatus;
import com.core.vdesk.domain.payments.repository.OrderRepository;
import com.core.vdesk.domain.payments.repository.PaymentRepository;
import com.core.vdesk.domain.payments.repository.ProductRepository;
import com.core.vdesk.domain.payments.repository.UserPlanRepository;
import com.core.vdesk.domain.users.UserRepository;
import com.core.vdesk.domain.users.Users;
import com.core.vdesk.domain.welcome.dto.WelcomeApproveResponseDto;
import com.core.vdesk.domain.welcome.dto.WelcomePayReadyResponseDto;
import com.core.vdesk.domain.welcome.entity.WelcomeProperties;
import com.core.vdesk.domain.welcome.util.WelcomeSignatureUtil;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserPlanRepository userPlanRepository;
    private final ObjectMapper om;

    private final WelcomeProperties welcomeProperties;
    private final WelcomeSignatureUtil welcomeSignatureUtil;
    private final WelcomeClient welcomeClient;

    /**
     * [1단계] 주문 생성 + 웰컴 결제창 호출 파라미터 생성
     * 총 결제 금액 = monthlyAmount × quantity × durationMonths
     */
    @Transactional
    public WelcomePayReadyResponseDto createWelcomeOrderAndParams(
            OrderCreateRequestDto req,
            Long userId,
            String buyerName,
            String buyerTel,
            String buyerEmail
    ) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        ProductCode code;
        try {
            code = ProductCode.valueOf(req.getProductCode());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 productCode: " + req.getProductCode());
        }

        Product product = productRepository.findByProductCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다. productCode=" + code));

        int quantity = req.getQuantity();
        int duration = req.getDurationMonths();
        long unitMonthlyAmount = product.getMonthlyAmount();
        long totalAmount = unitMonthlyAmount * quantity * duration;

        String goodname = "Vdesk " + product.getName() + " " + quantity + "\ub300 / " + duration + "\uac1c\uc6d4";
        String orderId = generateOrderId();

        Orders o = new Orders();
        o.setOrderId(orderId);
        o.setUser(user);
        o.setProduct(product);
        o.setAmount(totalAmount);
        o.setQuantity(quantity);
        o.setDurationMonths(duration);
        o.setPayMethod("Card");
        o.setType(OrderType.GENERAL);
        o.setStatus(OrderStatus.PENDING);
        orderRepository.save(o);

        String price = String.valueOf(totalAmount);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = welcomeSignatureUtil.createRequestSignature(orderId, price, timestamp);

        String returnUrl = welcomeProperties.getSiteDomain() + welcomeProperties.getReturnPath();
        String closeUrl  = welcomeProperties.getSiteDomain() + welcomeProperties.getClosePath();

        return WelcomePayReadyResponseDto.builder()
                .paymentType("ONE_TIME")
                .productCode(code.name())
                .productName(product.getName())
                .quantity(quantity)
                .durationMonths(duration)
                .unitMonthlyAmount(unitMonthlyAmount)
                .totalAmount(totalAmount)
                .version("1.0")
                .mid(welcomeProperties.getMid())
                .oid(orderId)
                .goodname(goodname)
                .price(price)
                .currency("WON")
                .buyername(buyerName)
                .buyertel(buyerTel)
                .buyeremail(buyerEmail)
                .timestamp(timestamp)
                .signature(signature)
                .returnUrl(returnUrl)
                .closeUrl(closeUrl)
                .mKey(welcomeProperties.getMKey())
                .gopaymethod("Card")
                .charset("UTF-8")
                .payViewType("overlay")
                .build();
    }

    /**
     * [2단계] 웰컴 인증 완료 후 서버-서버 승인 수행 및 DB 반영
     */
    @Transactional
    public void confirmWelcome(String orderId, String authToken, String authUrl, String netCancelUrl) {

        Orders order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문이 존재하지 않습니다."));

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("이미 PAID 처리된 주문입니다. orderId={}", orderId);
            return;
        }

        Long userId = order.getUser().getUserId();
        String price = String.valueOf(order.getAmount());

        String raw = welcomeClient.approveRaw(authUrl, authToken, price);
        log.info("승인 응답 원문 raw: {}", raw);

        WelcomeApproveResponseDto res;
        try {
            res = om.readValue(raw, WelcomeApproveResponseDto.class);
        } catch (Exception e) {
            tryNetCancel(netCancelUrl, authToken, price);
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "웰컴 승인 응답 파싱 실패", e);
        }

        log.info("웰컴 승인 결과 : resultCode={}, resultMsg={}, tid={}, price={}",
                res.getResultCode(), res.getResultMsg(), res.getTid(), price);

        boolean success = "0000".equals(res.getResultCode());
        if (!success) {
            saveFail(order, raw, res);
            throw new BusinessException(ErrorCode.PAYMENTS_API_HTTP_ERROR, "결제가 실패했습니다. 다시 시도해 주세요.");
        }

        if (res.getTid() != null && paymentRepository.existsByPgPaymentKey(res.getTid())) {
            log.info("이미 저장된 tid. 중복 처리 방지. tid={}, orderId={}", res.getTid(), orderId);
            order.setStatus(OrderStatus.PAID);
            order.setFailureReason(null);
            orderRepository.save(order);
            return;
        }

        try {
            Payment pay = saveSuccess(order, raw, res);
            applyUserPlan(order, pay);
        } catch (Exception e) {
            tryNetCancel(netCancelUrl, authToken, price);
            throw e;
        }
    }

    private Instant parseApprovedAt(String yyyyMMdd, String hhmmss) {
        if (yyyyMMdd == null || hhmmss == null) return null;
        try {
            LocalDate date = LocalDate.parse(yyyyMMdd, DateTimeFormatter.BASIC_ISO_DATE);
            LocalTime time = LocalTime.parse(hhmmss, DateTimeFormatter.ofPattern("HHmmss"));
            return ZonedDateTime.of(date, time, ZoneId.of("Asia/Seoul")).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    /**
     * 일반결제 승인 후 UserPlan 반영
     * - planType: productCode 기준
     * - expiresAt: now + durationMonths
     * - addonSessionCount: quantity (PC 수량)
     */
    private void applyUserPlan(Orders order, Payment pay) {
        Users user = order.getUser();

        UserPlan up = userPlanRepository.findByUser_UserId(user.getUserId())
                .orElseGet(UserPlan::new);

        ProductCode productCode = order.getProduct().getProductCode();
        PlanType planType;
        try {
            planType = PlanType.valueOf(productCode.name());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "planType 변환 실패: " + productCode);
        }

        int durationMonths = order.getDurationMonths() != null ? order.getDurationMonths() : 1;
        int quantity = order.getQuantity() != null ? order.getQuantity() : 1;

        Instant expiresAt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
                .plusMonths(durationMonths)
                .toInstant();

        up.setUser(user);
        up.setPayment(pay);
        up.setPlanType(planType);
        up.setBillingProvider(null);   // 일반결제: 빌링 없음
        up.setBillingId(null);
        up.setPaymentStatus(PaymentStatus.ACTIVE);
        up.setExpiresAt(expiresAt);
        up.setNextChargeDate(null);    // 일반결제: 자동 재청구 없음
        up.setAddonSessionCount(quantity);
        up.setAddonExpiresAt(expiresAt);
        userPlanRepository.save(up);
    }

    private Payment saveSuccess(Orders order, String raw, WelcomeApproveResponseDto res) {
        Payment pay = new Payment();
        pay.setOrder(order);
        pay.setProvider(PayProvider.WELCOME);
        pay.setPaidAmount(order.getAmount());
        pay.setCurrency("WON");
        pay.setPgPaymentKey(res.getTid());
        pay.setApprovedAt(parseApprovedAt(res.getApplDate(), res.getApplTime()));
        pay.setStatus(TxStatus.APPROVED);
        pay.setRawResponse(raw);
        paymentRepository.save(pay);

        order.setStatus(OrderStatus.PAID);
        order.setFailureReason(null);
        orderRepository.save(order);

        return pay;
    }

    private void saveFail(Orders order, String raw, WelcomeApproveResponseDto res) {
        Payment pay = new Payment();
        pay.setOrder(order);
        pay.setProvider(PayProvider.WELCOME);
        pay.setPaidAmount(order.getAmount());
        pay.setCurrency("WON");
        pay.setPgPaymentKey(res.getTid());
        pay.setApprovedAt(parseApprovedAt(res.getApplDate(), res.getApplTime()));
        pay.setStatus(TxStatus.FAILED);
        pay.setRawResponse(raw);
        paymentRepository.save(pay);

        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason("웰컴 결제 실패: " + res.getResultCode() + " - " + res.getResultMsg());
        orderRepository.save(order);
    }

    private void tryNetCancel(String netCancelUrl, String authToken, String price) {
        if (netCancelUrl == null || netCancelUrl.isBlank()) return;
        try {
            welcomeClient.netCancelRaw(netCancelUrl, authToken, price);
            log.warn("웰컴 망취소 호출 완료. netCancelUrl={}", netCancelUrl);
        } catch (Exception e) {
            log.warn("웰컴 망취소 호출 실패. netCancelUrl={}, msg={}", netCancelUrl, e.getMessage());
        }
    }

    private String generateOrderId() {
        return "ORDER-" + System.currentTimeMillis();
    }
}
