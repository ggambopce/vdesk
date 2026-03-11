package com.core.vdesk.domain.welcome.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.core.vdesk.domain.welcome.dto.WelcomeBillingChargeRequestDto;
import com.core.vdesk.domain.welcome.dto.WelcomeBillingChargeResponseDto;
import com.core.vdesk.domain.welcome.dto.WelcomeBillingIssueParamsResponseDto;
import com.core.vdesk.domain.welcome.dto.WelcomeBillingProps;
import com.core.vdesk.domain.welcome.dto.WelcomeBillingTargetDto;
import com.core.vdesk.domain.welcome.dto.WelcomeMobileResultDto;
import com.core.vdesk.domain.welcome.entity.BillingKeyStatus;
import com.core.vdesk.domain.welcome.entity.WelcomeBillingKey;
import com.core.vdesk.domain.welcome.repository.WelcomeBillingKeyRepository;
import com.core.vdesk.domain.welcome.util.WelcomeMobileSignature;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeBillingService {

    private final WelcomeBillingProps props;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserPlanRepository userPlanRepository;

    private final ObjectMapper om;
    private final WelcomeBillingClient welcomeBillingClient; // approveRaw + billpay

    private final WelcomeBillingKeyRepository welcomeBillingKeyRepository;

    // ------------------------------------------------------------------------
    // 결제창 오픈 파라미터 (issue-params)
    // ------------------------------------------------------------------------
    public WelcomeBillingIssueParamsResponseDto issueParms(Users user, String productCode, Integer quantity) {

        if (user == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, "user 누락");
        if (isBlank(productCode)) throw new BusinessException(ErrorCode.INVALID_REQUEST, "productCode 누락");

        Product product = productRepository.findByProductCode(ProductCode.valueOf(productCode))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다. productCode=" + productCode));

        Long amount = product.getMonthlyAmount() * Math.max(1, quantity == null ? 1 : quantity);
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "상품 금액이 올바르지 않습니다. amount=" + amount);
        }

        String ts = String.valueOf(System.currentTimeMillis());
        // MID_userId_ts (콜백에서 userId 추출용)
        String oid = "WELCOME-BILLING-SETUP_" + user.getUserId() + "_" + ts;

        // Orders를 미리 만들어둔다 (콜백에서 product/amount 복구 필요 없게)
        Orders o = new Orders();
        o.setOrderId(oid);
        o.setUser(user);
        o.setProduct(product);
        o.setAmount(amount);
        o.setQuantity(quantity);
        o.setDurationMonths(1);
        o.setPayMethod("CARD");
        o.setType(OrderType.BILLING);      // 기존 정책 유지
        o.setStatus(OrderStatus.PENDING);  // 결제 전이므로 PENDING
        orderRepository.save(o);

        String pAmt = String.valueOf(amount);
        String pTimestamp = ts;

        String pSignature = WelcomeMobileSignature.signature(props.getSignKey(), pAmt, oid, pTimestamp);
        String pReserved = "card_bill=Y&twotrs_isp=Y&block_isp=Y&apprun_check=Y";
        String nextUrl = props.getNextUrl();

        Map<String, String> f = new LinkedHashMap<>();
        f.put("P_MID", props.getMid());
        f.put("P_OID", oid);
        f.put("P_AMT", pAmt);
        f.put("P_UNAME", "ONEDESK");

        f.put("P_TIMESTAMP", pTimestamp);
        f.put("P_SIGNATURE", pSignature);
        f.put("P_RESERVED", pReserved);
        f.put("P_NEXT_URL", nextUrl);

        WelcomeBillingIssueParamsResponseDto res = new WelcomeBillingIssueParamsResponseDto();
        res.setActionUrl("https://" + props.getMobileHost() + props.getMobileCardPath());
        res.setFields(f);

        log.info("[BILLING-ISSUE] userId={}, productCode={}, oid={}, amt={}",
                user.getUserId(), productCode, oid, amount);

        return res;
    }

    // ------------------------------------------------------------------------
    // 콜백 + billkey 저장 + 첫 결제 PayAPI billpay + Orders/Payment/UserPlan 반영
    // - 선택지 A: productCode는 P_GOODS 없으면 P_NOTI로 복구
    // - “동작 성공” 기준: Orders/Payment/UserPlan 상태 + 로그
    // ------------------------------------------------------------------------
    @Transactional
    public void handleBillKeyCallbackAndFirstCharge(WelcomeMobileResultDto r, Map<String, String> callbackRaw) {

        // [LOG #1] 콜백 진입(최소 증빙)
        String cbStatus = firstNonBlank(val(callbackRaw, "P_STATUS"), val(r, "P_STATUS"));
        String cbOid = firstNonBlank(val(callbackRaw, "P_OID"), val(r, "P_OID"));
        String cbTid = firstNonBlank(val(callbackRaw, "P_TID"), val(r, "P_TID"));
        String cbReqUrl = firstNonBlank(val(callbackRaw, "P_REQ_URL"), val(r, "P_REQ_URL"));
        String cbAmt = firstNonBlank(val(callbackRaw, "P_AMT"), val(r, "P_AMT"));

        log.info("[BILLKEY-CB] status={}, oid={}, tid={}, reqUrl={}, amt={}",
                nz(cbStatus), nz(cbOid), nz(cbTid), nz(cbReqUrl), nz(cbAmt));

        if (!"00".equals(cbStatus)) {
            String msg = firstNonBlank(val(callbackRaw, "P_RMESG1"), val(r, "P_RMESG1"));
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "모바일 결제 인증 실패 status=" + nz(cbStatus) + ", msg=" + nz(msg));
        }

        Map<String, String> finalMap = new LinkedHashMap<>();
        if (callbackRaw != null) finalMap.putAll(callbackRaw);

        // billkey가 콜백에 없으면 approve로 보강
        String billkey = firstNonBlank(val(callbackRaw, "P_CARD_BILLKEY"), val(r, "P_CARD_BILLKEY"));
        if (!isBlank(billkey)) {
            log.info("빌키 획득 경로 : 콜백 리다이렉트. oid={}, tid={}", nz(cbOid), nz(cbTid));
        }

        if (isBlank(billkey)) {

            String reqUrl = firstNonBlank(val(callbackRaw, "P_REQ_URL"), val(r, "P_REQ_URL"));
            String tid = firstNonBlank(val(callbackRaw, "P_TID"), val(r, "P_TID"));

            if (isBlank(reqUrl) || isBlank(tid)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST,
                        "approve가 필요한데 P_REQ_URL/P_TID가 없습니다. raw=" + String.valueOf(callbackRaw));
            }

            String approveRaw = welcomeBillingClient.approveRaw(reqUrl, props.getMid(), tid);
            Map<String, String> approveMap = parseWelcomeResponseToMap(approveRaw);
            finalMap.putAll(approveMap);

            billkey = finalMap.get("P_CARD_BILLKEY");
            log.info("빌키 획득 경로 : 승인원문 요청 파싱. oid={}, tid={}", nz(cbOid), nz(cbTid));
        }

        // 필수값 검증
        String oid = finalMap.get("P_OID");
        if (isBlank(oid)) throw new BusinessException(ErrorCode.INVALID_REQUEST, "P_OID 누락");
        if (isBlank(billkey))
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "P_CARD_BILLKEY 누락(approve 후에도 없음)");

        // Orders를 oid로 조회해서 상품/금액 확정
        Orders o = orderRepository.findByOrderId(oid)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "주문 없음 oid=" + oid));

        Users user = o.getUser();
        if (user == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, "주문에 사용자 없음 oid=" + oid);

        Product product = o.getProduct();
        if (product == null) throw new BusinessException(ErrorCode.INVALID_REQUEST, "주문에 상품 없음 oid=" + oid);

        Long amount = o.getAmount();
        if (amount == null || amount <= 0) throw new BusinessException(ErrorCode.INVALID_REQUEST, "주문 금액 오류 oid=" + oid);

        // billkey upsert
        WelcomeBillingKey bk = welcomeBillingKeyRepository.findByUser_UserId(user.getUserId()).orElseGet(WelcomeBillingKey::new);
        bk.setUser(user);
        bk.setBillingKey(billkey);
        bk.setCardCode(finalMap.get("P_FN_CD1"));
        bk.setCardNumHash(finalMap.get("P_CARD_NUMHASH"));
        bk.setStatus(BillingKeyStatus.ACTIVE);
        bk.setUpdatedAt(Instant.now());
        try {
            bk.setLastResultRaw(om.writeValueAsString(finalMap));
        } catch (Exception e) {
            bk.setLastResultRaw(String.valueOf(finalMap));
        }
        welcomeBillingKeyRepository.save(bk);

        // 빌키 확보 확인 로그
        log.info("[BILLKEY-OK] oid={}, userId={}, productCode={}, billkey={}, cardCode={}, cardHash={}",
                oid, user.getUserId(), product.getProductCode(), mask(billkey), nz(bk.getCardCode()), nz(bk.getCardNumHash()));


        // billpay 호출
        WelcomeBillingChargeRequestDto req = new WelcomeBillingChargeRequestDto();
        req.setOid(o.getOrderId());
        req.setPrice(String.valueOf(amount));
        req.setBillkey(billkey);
        req.setGoodsName(product.getName());
        req.setBuyerName(user.getUserName());
        req.setBuyerTel("01077771004");
        req.setBuyerEmail(user.getEmail());

        WelcomeBillingChargeResponseDto payRes = welcomeBillingClient.billpay(req);
        String payRaw = payRes == null ? null : payRes.getRawResponse();

        // Payment 저장
        Payment p = new Payment();
        p.setOrder(o);
        p.setProvider(PayProvider.WELCOME_BILLING);
        p.setCurrency("WON");
        p.setPaidAmount(amount);
        p.setBillingKey(billkey);
        p.setRawResponse(payRaw);

        // 멱등 최소: pgKey가 있으면 중복이면 스킵
        String pgKey = extractPayApiPgKey(payRaw);
        if (!isBlank(pgKey) && paymentRepository.existsByPgPaymentKey(pgKey)) {
            log.info("[BILLPAY-IDEMPOTENT] duplicated pgKey={}, skip. orderId={}", pgKey, o.getOrderId());
        }
        p.setPgPaymentKey(isBlank(pgKey) ? null : pgKey);

        boolean success = isBillpaySuccess(payRaw);
        p.setStatus(success ? TxStatus.APPROVED : TxStatus.FAILED);
        p.setApprovedAt(success ? Instant.now() : null);
        paymentRepository.save(p);

        // Orders 업데이트
        if (success) {
            o.setStatus(OrderStatus.PAID);
            o.setFailureReason(null);
        } else {
            o.setStatus(OrderStatus.FAILED);
            o.setFailureReason("PAYAPI_BILLPAY_FAIL: " + compact(payRaw));
        }
        orderRepository.save(o);

        // 최초청구 결과 확정(이 한 줄로 성공/실패 판단)
        log.info("웰컴 스탠다드 빌링결제 최초 청구 성공 결과 orderId={}, success={}, pgKey={}, amount={}, raw={}",
                o.getOrderId(), success, nz(pgKey), amount, compact(payRaw));

        if (!success) {
            throw new BusinessException(
                    ErrorCode.PAYMENTS_API_HTTP_ERROR,
                    "첫 결제 청구(billpay) 실패 orderId=" + o.getOrderId() + ", raw=" + compact(payRaw)
            );
        }

        // UserPlan 활성화
        UserPlan up = userPlanRepository.findByUser_UserId(user.getUserId()).orElseGet(UserPlan::new);
        up.setUser(user);
        up.setPayment(p);

        up.setBillingProvider(BillingProvider.WELCOME);
        up.setBillingId(billkey);
        PlanType planType = toPlanType(product);
        up.setPlanType(planType);

        Instant expiresAt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
                .plusMonths(1)
                .toInstant();
        Instant nextChargeDate = expiresAt.minus(1, ChronoUnit.DAYS);

        up.setPaymentStatus(PaymentStatus.ACTIVE);
        up.setExpiresAt(expiresAt);
        up.setNextChargeDate(nextChargeDate);
        if (up.getAddonSessionCount() > 0) {
            up.setAddonExpiresAt(expiresAt);
        } else {
            up.setAddonExpiresAt(null);
        }
        userPlanRepository.save(up);

    }

    /**
     * [정기 결제] 스케줄러가 대상(만료/결제 예정 도래)을 조회한 뒤, 각 대상에 대해 재청구를 수행한다.
     * 처리 흐름
     * 1) DTO 최소 가드(활성 상태/청구일 도래)
     * 2) UserPlan 로드 후 재검증(DB 상태가 진실)
     * 3) WelcomeBillingKey 로드 및 ACTIVE 여부 확인
     * 4) 상품 조회
     * 5) Orders 생성(PENDING)
     * 6) billpay 호출
     * 7) Payment 저장(성공/실패 기록)
     * 8) Orders 업데이트
     * 9) 성공이면 UserPlan 갱신(nextChargeDate, expiresAt, payment 연결)
     * 10) 실패면 실패 정책 적용(FAILED 유지 / 즉시 취소 / 재시도 설계 등)
     *
     * 멱등성
     * - tid 중복 시 스킵(재시도/네트워크 중복 호출 대비)
     */
    @Transactional
    public void charge(WelcomeBillingTargetDto target) {
        if (target == null) return;

        // 1) DTO 최소 가드
        if (target.getUserId() == null) return;
        if (target.getNextChargeDate() == null) return;
        if (target.getPaymentStatus() != PaymentStatus.ACTIVE) return;
        if (target.getStatus() != BillingKeyStatus.ACTIVE) return;
        if (isBlank(target.getBillingKey())) return;

        Instant now = Instant.now();
        if (target.getNextChargeDate().isAfter(now)) {
            // 아직 결제일 도래 전
            return;
        }

        Long userId = target.getUserId();

        // 2) User / UserPlan 로드 후 재검증
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자 없음 userId=" + userId));

        UserPlan up = userPlanRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "유저 플랜 없음 userId=" + userId));

        if (up.getPaymentStatus() != PaymentStatus.ACTIVE) {
            log.info("[BILLING-CHARGE] skip. userId={}, planStatus={}", userId, up.getPaymentStatus());
            return;
        }
        if (up.getNextChargeDate() == null || up.getNextChargeDate().isAfter(now)) {
            log.info("[BILLING-CHARGE] skip. userId={}, nextChargeDate={}", userId, up.getNextChargeDate());
            return;
        }

        // 빌링키 로드 및 검증
        WelcomeBillingKey bk = welcomeBillingKeyRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "빌링키 없음 userId=" + userId));
        if (bk.getStatus() != BillingKeyStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "빌링키 비활성 userId=" + userId);
        }
        String billkey = bk.getBillingKey();
        if (isBlank(billkey)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "빌링키 값 없음 userId=" + userId);
        }

        // 플랜 기반 상품 결정
        PlanType planType = up.getPlanType();
        if (planType == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "planType 누락 userId=" + userId);
        }

        ProductCode baseProductCode = ProductCode.valueOf(planType.name());
        Product baseProduct = productRepository.findByProductCode(baseProductCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "상품 없음 productCode=" + baseProductCode));

        Long baseAmount = baseProduct.getMonthlyAmount();
        if (baseAmount == null || baseAmount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "상품 금액 오류 amount=" + baseAmount);
        }

        // 수량 기반 청구: monthlyAmount * addonSessionCount(VM 대수)
        int addonCount = Math.max(1, up.getAddonSessionCount());
        long totalAmount = baseAmount * addonCount;
        if (totalAmount <= 0) throw new BusinessException(ErrorCode.INVALID_REQUEST, "청구 금액이 0원 이하입니다.");

        // 5) Orders 생성(PENDING) - 멱등 orderId 생성(유저+결제예정일 기준)
        // 같은 대상이면 같은 orderId가 나오므로 중복 결제/중복 저장을 줄인다.
        String orderId = buildRecurringOrderId(userId, up.getNextChargeDate());

        // 이미 같은 orderId로 결제가 APPROVED 된 이력이 있으면 스킵(강한 멱등)
        if (paymentRepository.existsByOrder_OrderIdAndStatus(orderId, TxStatus.APPROVED)) {
            log.info("[BILLING-CHARGE] already approved. skip. userId={}, orderId={}", userId, orderId);
            // 이미 처리된 건이라면 nextChargeDate 밀어주는 정책을 택할 수도 있지만,
            // 여기서는 "이미 성공 결제 존재"를 신뢰하고 스킵만 한다.
            return;
        }

        Orders o = orderRepository.findByOrderId(orderId).orElse(null);
        if (o == null) {
            o = new Orders();
            o.setOrderId(orderId);
            o.setUser(user);
            o.setProduct(baseProduct);
            o.setAmount(totalAmount);
            o.setPayMethod("CARD");
            o.setType(OrderType.BILLING);
            o.setStatus(OrderStatus.PENDING);
            orderRepository.save(o);
        } else {
            // 이미 생성되어 있는데 PAID면 스킵
            if (o.getStatus() == OrderStatus.PAID) {
                log.info("[BILLING-CHARGE] order already paid. skip. userId={}, orderId={}", userId, orderId);
                return;
            }
            // FAILED/PENDING이면 재시도 허용(정책 선택)
            o.setStatus(OrderStatus.PENDING);
            o.setFailureReason(null);
            o.setAmount(totalAmount);
            orderRepository.save(o);
        }

        // 6) billpay 호출
        WelcomeBillingChargeRequestDto req = new WelcomeBillingChargeRequestDto();
        req.setOid(o.getOrderId());
        req.setPrice(String.valueOf(totalAmount));
        req.setBillkey(billkey);
        String goodsName = baseProduct.getName() + " " + addonCount + "대";
        req.setGoodsName(goodsName);
        req.setBuyerName(user.getUserName());
        req.setBuyerTel("01077771004");
        req.setBuyerEmail(user.getEmail());

        WelcomeBillingChargeResponseDto payRes = welcomeBillingClient.billpay(req);
        String payRaw = payRes == null ? null : payRes.getRawResponse();

        // 7) Payment 저장(성공/실패 기록)
        Payment p = new Payment();
        p.setOrder(o);
        p.setProvider(PayProvider.WELCOME_BILLING);
        p.setCurrency("WON");
        p.setPaidAmount(totalAmount);
        p.setBillingKey(billkey);
        p.setRawResponse(payRaw);

        String pgKey = extractPayApiPgKey(payRaw);

        // pgKey 기준 멱등: 이미 같은 pgKey 있으면 저장 스킵/종료
        if (!isBlank(pgKey) && paymentRepository.existsByPgPaymentKey(pgKey)) {
            log.info("[BILLPAY-IDEMPOTENT] duplicated pgKey={}, skip save. orderId={}", pgKey, o.getOrderId());
            // 이미 처리된 건이므로 order 상태만 보정 시도
            o.setStatus(OrderStatus.PAID);
            orderRepository.save(o);
            return;
        }

        p.setPgPaymentKey(isBlank(pgKey) ? null : pgKey);

        boolean success = isBillpaySuccess(payRaw);
        p.setStatus(success ? TxStatus.APPROVED : TxStatus.FAILED);
        p.setApprovedAt(success ? Instant.now() : null);
        paymentRepository.save(p);

        // 8) Orders 업데이트
        if (success) {
            o.setStatus(OrderStatus.PAID);
            o.setFailureReason(null);
        } else {
            o.setStatus(OrderStatus.FAILED);
            o.setFailureReason("PAYAPI_BILLPAY_FAIL: " + compact(payRaw));
        }
        orderRepository.save(o);

        log.info("[BILLING-CHARGE] userId={}, orderId={}, success={}, baseAmount={}, addonCount={}, totalAmount={}, pgKey={}",
                userId, o.getOrderId(), success, baseAmount, addonCount, totalAmount, nz(pgKey));

        // 9) 성공이면 UserPlan 갱신(nextChargeDate, expiresAt, payment 연결)
        if (success) {
            Instant expiresAt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
                    .plusMonths(1)
                    .toInstant();
            Instant nextChargeDate = expiresAt.minus(1, ChronoUnit.DAYS);

            up.setPayment(p);
            up.setPaymentStatus(PaymentStatus.ACTIVE);
            up.setExpiresAt(expiresAt);
            up.setNextChargeDate(nextChargeDate);
            up.setPlanType(planType);

            if (up.getAddonSessionCount() > 0) {
                up.setAddonExpiresAt(expiresAt);
            } else {
                // 선택: 애드온이 없으면 만료일도 비움(데이터 정합성)
                up.setAddonExpiresAt(null);
            }

            userPlanRepository.save(up);
            return;
        }

        // 10) 실패 정책(기본)
        // - ACTIVE -> FAILED 로 내리고, nextChargeDate를 유지하면 "다음 스케줄에서 계속 실패 대상"이 된다.
        // - 운영상 재시도 정책을 붙이려면 "nextChargeDate + 6시간" 같은 방식으로 미루는 게 일반적이다.
        // 여기서는 기본값으로 FAILED 전환 + nextChargeDate는 그대로 둔다.
        up.setPayment(p);
        up.setPaymentStatus(PaymentStatus.FAILED);
        userPlanRepository.save(up);

        throw new BusinessException(
                ErrorCode.PAYMENTS_API_HTTP_ERROR,
                "정기 결제(billpay) 실패 userId=" + userId + ", orderId=" + o.getOrderId() + ", raw=" + compact(payRaw)
        );
    }

    /**
     * 재청구용 orderId (멱등 키)
     * - 같은 userId + 같은 nextChargeDate 라면 항상 동일한 orderId가 생성된다.
     */
    private String buildRecurringOrderId(Long userId, Instant nextChargeDate) {
        long t = nextChargeDate.toEpochMilli();
        return "WELCOME-BILLING-RECUR-" + userId + "-" + t;
    }

    // -------------------- 유틸 --------------------

    private static String val(Map<String, String> m, String k) { return m == null ? null : m.get(k); }
    private static String val(WelcomeMobileResultDto r, String k) {
        if (r == null) return null;
        // WelcomeMobileResult가 getter를 갖고 있으니 여기서는 필요한 키만 직접 쓰는 게 안전
        // (이 메서드는 cb 로그용으로만 사용)
        if ("P_STATUS".equals(k)) return r.getP_STATUS();
        if ("P_OID".equals(k)) return r.getP_OID();
        if ("P_TID".equals(k)) return r.getP_TID();
        if ("P_AMT".equals(k)) return r.getP_AMT();
        if ("P_RMESG1".equals(k)) return r.getP_RMESG1();
        if ("P_CARD_BILLKEY".equals(k)) return r.getP_CARD_BILLKEY();
        return null;
    }

    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) {
            if (s != null && !s.trim().isEmpty()) return s.trim();
        }
        return null;
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static boolean isBlank(String s){ return s == null || s.trim().isEmpty(); }

    /**
     * Product -> PlanType 변환
     * - ProductCode enum name이 PlanType enum name과 동일하다는 전제
     */
    private PlanType toPlanType(Product product) {
        if (product == null || product.getProductCode() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "productCode 누락");
        }
        String code = product.getProductCode().name(); // productCode가 String이면 여기만 product.getProductCode()로 바꾸면 됨
        try {
            return PlanType.valueOf(code);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "planType 변환 실패 productCode=" + code);
        }
    }

    private Map<String, String> parseWelcomeResponseToMap(String raw) {
        raw = raw == null ? "" : raw.trim();

        if (raw.startsWith("{")) {
            try {
                Map<String, Object> obj = om.readValue(raw, new TypeReference<Map<String, Object>>() {});
                Map<String, String> out = new LinkedHashMap<>();
                for (Map.Entry<String, Object> e : obj.entrySet()) {
                    out.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
                }
                return out;
            } catch (Exception e) {
                throw new IllegalStateException("approve response json parse failed: " + raw, e);
            }
        }

        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String k = pair.substring(0, idx).trim();
            String v = pair.substring(idx + 1).trim();
            map.put(k, v);
        }
        return map;
    }

    private boolean isBillpaySuccess(String raw) {
        if (isBlank(raw)) return false;
        String r = raw.trim();

        if (r.startsWith("{")) {
            try {
                Map<String, Object> m = om.readValue(r, new TypeReference<Map<String, Object>>() {});
                String code = pick(m, "ResultCode", "resultCode", "code");
                return "00".equals(code) || "0000".equals(code) || "200".equals(code);
            } catch (Exception ignore) {}
        }

        if (r.contains("ResultCode=")) {
            String code = extractNvp(r, "ResultCode");
            return "00".equals(code) || "0000".equals(code);
        }
        if (r.contains("resultCode=")) {
            String code = extractNvp(r, "resultCode");
            return "00".equals(code) || "0000".equals(code);
        }

        return r.contains("\"code\":200") || r.contains("\"code\":\"200\"");
    }

    private String extractPayApiPgKey(String raw) {
        if (isBlank(raw)) return null;
        String r = raw.trim();

        if (r.startsWith("{")) {
            try {
                Map<String, Object> m = om.readValue(r, new TypeReference<Map<String, Object>>() {});
                return pick(m, "tid", "TID", "Tid", "transactionId", "TransactionId", "payKey", "paymentKey");
            } catch (Exception ignore) {}
        }

        String tid = extractNvp(r, "tid");
        if (!isBlank(tid)) return tid;

        tid = extractNvp(r, "Tid");
        if (!isBlank(tid)) return tid;

        return null;
    }

    private String pick(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }

    private String extractNvp(String raw, String key) {
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            String k = pair.substring(0, idx).trim();
            if (!key.equals(k)) continue;
            return pair.substring(idx + 1).trim();
        }
        return null;
    }

    private String compact(String s) {
        if (s == null) return null;
        s = s.replace("\n", " ").replace("\r", " ").trim();
        return s.length() > 600 ? s.substring(0, 600) + "..." : s;
    }

    private String mask(String s) {
        if (isBlank(s)) return "****";
        int n = s.length();
        if (n <= 8) return "****";
        return s.substring(0, 4) + "****" + s.substring(n - 4);
    }

    public List<WelcomeBillingTargetDto> findBillingTargets(Instant now) {
        return welcomeBillingKeyRepository.findWelcomeBillingTargets(now);
    }

    public void cancelBilling(Long userId) {
        UserPlan up = userPlanRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "플랜을 찾을 수 없습니다."));

        if (up.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 해지된 플랜입니다.");
        }

        // UserPlan 해지 처리, 만료 날짜는 유지
        up.setPaymentStatus(PaymentStatus.CANCEL_SCHEDULED);
        up.setNextChargeDate(null);
        userPlanRepository.save(up);

        // WelcomeBillingKey도 비활성화 (스케줄러 청구 차단)
        welcomeBillingKeyRepository.findByUser_UserId(userId).ifPresent(key -> {
            if (key.getStatus() == BillingKeyStatus.ACTIVE) {
                key.setStatus(BillingKeyStatus.INACTIVE);
                welcomeBillingKeyRepository.save(key);
            }
        });
    }

    public void resumeBilling(Long userId) {
        UserPlan up = userPlanRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "플랜 없음"));

        // CANCEL_SCHEDULED 상태만 재개 허용 (EXPIRED/FAILED/ACTIVE 등은 차단)
        if (up.getPaymentStatus() != PaymentStatus.CANCEL_SCHEDULED) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "CANCEL_SCHEDULED 상태의 플랜만 재개할 수 있습니다. current=" + up.getPaymentStatus()
            );
        }

        // 만료가 아직 남아있는 경우: 즉시 과금 없이 nextChargeDate만 복구
        Instant now = Instant.now();
        if (up.getExpiresAt() != null && up.getExpiresAt().isAfter(now)) {

            WelcomeBillingKey key = welcomeBillingKeyRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "빌링키 없음"));

            // 키가 죽어있다면 살리기(기존 cancel에서 INACTIVE로 만들었을 경우 대비)
            if (key.getStatus() != BillingKeyStatus.ACTIVE) {
                key.setStatus(BillingKeyStatus.ACTIVE);
                welcomeBillingKeyRepository.save(key);
            }


            up.setPaymentStatus(PaymentStatus.ACTIVE);
            up.setNextChargeDate(up.getExpiresAt()); // 만료 시점에 다음 결제
            // billingKey 동기화
            up.setBillingId(key.getBillingKey());
            userPlanRepository.save(up);

            return;
        }


        // 만료가 끝났으면: 신규 결제 플로우(카드등록/첫결제)로 유도
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "만료 후에는 신규 결제로 진행해야 합니다.");
    }


    /**
     * [만료 스윕 처리]
     * - PERSONAL 정기결제 플랜 중 "만료 시점(expiresAt)이 지난 사용자"를 일괄 정리한다.
     * - 스케줄러(예: 1일 1회 또는 1시간 1회)에서 호출되는 배치성 로직이다.
     * - 빌링 결제가 실패하거나, 사용자가 구독을 해지한 이후에도
     *   expiresAt 기준으로 서비스 접근이 허용되는 기간이 존재할 수 있다.
     * - 이 메서드는 해당 유예 기간이 종료된 사용자를 FREE 플랜으로 강등시키고,
     *   자동 결제 및 스케줄러 대상에서 완전히 제외하기 위한 "최종 정리 단계"다.
     * 주요 처리 대상
     * - planType = PERSONAL
     * - expiresAt <= now
     * - 아직 FREE로 강등되지 않은 사용자
     * 웰컴, 페이팔 빌링 결제 사용자 모두 관리
     */
    public int expireSweep(Instant now) {
        if (now == null) now = Instant.now();

        List<UserPlan> expiredPlans = userPlanRepository.findExpiredPlans(now);
        if (expiredPlans.isEmpty()) return 0;

        int count = 0;

        for (UserPlan up : expiredPlans) {
            // 혹시 이미 FREE로 정리됐으면 스킵(중복 방어)


            Long userId = up.getUser() != null ? up.getUser().getUserId() : null;

            // 플랜 강등

            up.setPaymentStatus(PaymentStatus.EXPIRED);
            up.setNextChargeDate(null);

            userPlanRepository.save(up);

            // 웰컴 빌링키도 비활성화
            if (userId != null) {
                welcomeBillingKeyRepository.findByUser_UserId(userId).ifPresent(key -> {
                    if (key.getStatus() == BillingKeyStatus.ACTIVE) {
                        key.setStatus(BillingKeyStatus.INACTIVE);
                        welcomeBillingKeyRepository.save(key);
                    }
                });
            }

            count++;
            log.info("웰컴, 페이팔 빌링결제 만료처리 완료 userId={}, expiresAt={}", userId, up.getExpiresAt());
        }

        return count;
    }
}

