package com.core.vdesk.domain.users;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.mail.MailException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.core.vdesk.domain.emails.EmailService;
import com.core.vdesk.domain.emails.InMemoryEmailVerificationStore;
import com.core.vdesk.domain.users.dto.LoginRequestDto;
import com.core.vdesk.domain.users.dto.MeProfileResponseDto;
import com.core.vdesk.domain.users.dto.ResetPasswordRequestDto;
import com.core.vdesk.domain.users.dto.SignupRequestDto;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;
import com.core.vdesk.global.jwt.AuthCookieUtil;
import com.core.vdesk.global.jwt.TokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InMemoryEmailVerificationStore emailStore;
    private final EmailService emailService;
    private final TokenService tokenService;
  
    private static final boolean SECURE_COOKIE = false; // 운영 HTTPS면 true


    /**
     * 일반로그인 로그인 처리
     * 1) 자격 검증 - 비밀번호와 디비 비밀번호 일치 확인
     * 2) 사용자 조회
     * 3) JWT 발급
     * 4) 쿠키 세팅
     */
    @Transactional
    public void login(LoginRequestDto req, HttpServletResponse res) {
        // 비밀번호와 디비 비밀번호 일치 확인
        verifyCredentials(req);

        // 사용자 조회
        Users user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // JWT 발급
        var pair = tokenService.issueAll(user.getEmail());

        // 쿠키 세팅
        AuthCookieUtil.writeAuthCookies(res, pair.at(), pair.rt(), SECURE_COOKIE);

    }

    /**
     * 회원가입 처리.
     * 1) 이메일 중복 검사
     * 2) 이메일 인증 여부 확인 (이메일 인증이 안되면 가입 불가)
     * 3) 비밀번호/비밀번호확인 일치 여부 확인
     * 4) Users 엔티티 생성 및 암호화된 비밀번호 저장
     * 5) DB에 저장 + 감사 로그
     * @param requestDto 회원가입 요청 데이터 (email, password, confirmPassword)
     */
    @Transactional
    public void signup(SignupRequestDto requestDto) {

        // 이메일 중복 검사
        if(userRepository.existsByEmail(requestDto.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 가입된 이메일입니다.");
        }
        if(!emailStore.isVerified(requestDto.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이메일 인증이 필요합니다.");
        }

        if(!requestDto.getPassword().equals(requestDto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        Users u = new Users();
        u.setEmail(requestDto.getEmail());
        u.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));
        u.setUserName(requestDto.getUserName());
        u.setRoles("ROLE_USER");
        u.setLoginType("normal");

        userRepository.save(u);

        // 가입 직후 FREE_PLAN 플랜 보장
        userPlanService.ensureFreePlanExists(u);

    }

    /**
     * 로그아웃 처리
     * 1) RT 쿠키 읽어서 서버측 로그아웃(블랙리스트/삭제) 처리
     * 2) AT/RT/FAM 쿠키 제거
     * 3) 세션 무효화 + SecurityContext 정리
     */
    public void logout(HttpServletRequest req, HttpServletResponse res) {

        // 1) 서버측 토큰 폐기 (RT가 있을 때만)
        String rt = AuthCookieUtil.readCookie(req, "RT");
        if (rt != null && !rt.isBlank()) {
            tokenService.logoutByRefreshToken(rt);
        }

        // 2) 클라이언트 쿠키 제거 (도메인/경로/보안 옵션은 기존 정책에 맞춤)
        AuthCookieUtil.deleteCookie(res, "AT", null, "/", SECURE_COOKIE, "Lax");
        AuthCookieUtil.deleteCookie(res, "RT", null, "/", SECURE_COOKIE, "Lax");
        AuthCookieUtil.deleteCookie(res, "FAM", null, "/", SECURE_COOKIE, "Lax");

        // 3) 세션/시큐리티 컨텍스트 정리 (무상태지만 방어적으로 처리)
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();

    }

    /**
     * 비밀번호와 디비 비밀번호 일치 확인
     */
    @Transactional
    public void verifyCredentials(@Valid LoginRequestDto req) {

        Users user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이메일을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
        }
    }

    /**
     * 이메일 중복 여부 확인.
     * @req email 사용자 이메일
     * @return true면 이미 존재함.
     */
    @Transactional
    public boolean isEmailDuplicated(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 인증코드를 생성하고 이메일로 전송한다.
     *  1) 6자리 난수 코드 생성 (000000~999999)
     *  2) InMemoryEmailVerificationStore 에 코드 저장 (유효기간 10분)
     *  3) MailService 를 통해 수신자 이메일로 발송
     */
    public void sendVerificationCode(String email) {

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        emailStore.save(email, code, Duration.ofMinutes(10));

        // Google Mail 호출 실패시 예외처리
        try {
            emailService.sendVerificationCode(email, code);
        } catch (MailException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "인증 메일 전송 중 오류가 발생했습니다.");
        }

    }

    /**
     * 사용자가 입력한 인증코드를 검증하고 일치 시 verified=true로 표시한다.
     * @param email 이메일 주소
     * @param code 사용자가 입력한 코드
     * @return 일치하면 true, 불일치/만료되면 false
     */
    public boolean verifyCode(String email, String code) {
        return emailStore.checkAndMarkVerified(email, code); // 일치 시 ture 저장
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto req) {
        // 비밀번호 확인
        if(!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        // 이메일 인증 여부 확인
        if(!emailStore.isVerified(req.getEmail())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이메일 인증이 필요합니다.");
        }
        Users user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "이메일에 해당하는 사용자를 찾을 수 없습니다."));

        // userName 검증
        if (!user.getUserName().equals(req.getUserName())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이메일과 사용자 이름이 일치하지 않습니다.");
        }

        // 비밀번호 암호화 후 저장
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public MeProfileResponseDto getMeProfile(Users user) {
        Users u = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));


    
        // FREE_PLAN이면 요금 관련 필드들은 null
        Long planAmount = null;
        Long addonAmount = null;
        Long nextChargeAmount = null;

        if (effectivePlan != PlanType.FREE_PLAN) {

            // 3) 플랜 기본 요금 (Product 테이블에서 조회)
            planAmount = findPlanAmount(effectivePlan);

            // 4) 애드온 개수(만료 정책 반영)
            int addonCount = 0;
            if (userPlan != null) {
                addonCount = userPlan.getAddonSessionCount();
                if (userPlan.getAddonExpiresAt() != null && userPlan.getAddonExpiresAt().isBefore(Instant.now())) {
                    addonCount = 0;
                }
            }

            // 5) 애드온 단가 및 애드온 요금
            long addonUnit = findAddonUnitAmount();
            addonAmount = addonUnit * (long) addonCount;

            // 6) 다음 결제 요금
            nextChargeAmount = planAmount + addonAmount;
        }

        // 7) DTO 조립 (planSessionCount는 PlanType.basicSessions 사용)
        return MeProfileResponseDto.of(
                u,
                userPlan,
                registeredDeviceCount,
                planAmount,
                addonAmount,
                nextChargeAmount,
                effectivePlan.getBasicSessions()
        );
    }

    private PlanType resolveEffectivePlan(UserPlan plan) {
        if (plan == null) return PlanType.FREE_PLAN;

        if (plan.getPaymentStatus() != PaymentStatus.ACTIVE) return PlanType.FREE_PLAN;

        if (plan.getExpiresAt() != null && plan.getExpiresAt().isBefore(Instant.now())) return PlanType.FREE_PLAN;

        return plan.getPlanType();
    }

    private long findPlanAmount(PlanType planType) {
        ProductCode code = switch (planType) {
            case SOLO_PLAN -> ProductCode.SOLO_PLAN;
            case PRO_PLAN -> ProductCode.PRO_PLAN;
            case TEAM_PLAN -> ProductCode.TEAM_PLAN;
            case BUSINESS_PLAN -> ProductCode.BUSINESS_PLAN;
            case TEST_PLAN -> ProductCode.TEST_PLAN;
            default -> null;
        };

        if (code == null) return 0L;

        Product p = productRepository.findByProductCode(code)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "플랜 요금 정보를 찾을 수 없습니다. productCode=" + code
                ));
        return p.getAmount();
    }

    private long findAddonUnitAmount() {
        Product p = productRepository.findByProductCode(ProductCode.ADDON_SESSION)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "애드온 요금 정보를 찾을 수 없습니다. productCode=ADDON_SESSION"
                ));
        return p.getAmount();
    }

    @Transactional
    public void withdraw(Users user, String password, HttpServletRequest request, HttpServletResponse response) {
        String loginType = user.getLoginType();

        if ("normal".equals(loginType)) {
            // 비밀번호 필수
            if (password == null || password.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "현재 비밀번호가 필요합니다.");
            }
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
            }
        } else if ("google".equals(loginType)) {
            // 소셜로그인은 비밀번호 검증 스킵
        } else {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 로그인 타입입니다.");
        }
        // 로그아웃
        logout(request, response);
        // 유저 도메인 데이터 정리
        softDeleteUser(user);

    }
    private void softDeleteUser(Users user) {
        String anonymizedEmail = "deleted-" + user.getUserId()+ "-" + user.getEmail();

        user.setEmail(anonymizedEmail);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());

        // 필요하면 도메인 상태 정리
        userRepository.save(user);
    }

}
