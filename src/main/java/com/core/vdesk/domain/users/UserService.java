package com.core.vdesk.domain.users;

import java.time.Duration;
import java.time.Instant;
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
     * 일반로그인 처리
     */
    @Transactional
    public void login(LoginRequestDto req, HttpServletResponse res) {
        verifyCredentials(req);

        Users user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        var pair = tokenService.issueAll(user.getEmail());
        AuthCookieUtil.writeAuthCookies(res, pair.at(), pair.rt(), SECURE_COOKIE);
    }

    /**
     * 회원가입 처리
     */
    @Transactional
    public void signup(SignupRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 가입된 이메일입니다.");
        }
        if (!emailStore.isVerified(requestDto.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이메일 인증이 필요합니다.");
        }
        if (!requestDto.getPassword().equals(requestDto.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        Users u = new Users();
        u.setEmail(requestDto.getEmail());
        u.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));
        u.setUserName(requestDto.getUserName());
        u.setRoles("ROLE_USER");
        u.setLoginType("normal");

        userRepository.save(u);
    }

    /**
     * 로그아웃 처리
     */
    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String rt = AuthCookieUtil.readCookie(req, "RT");
        if (rt != null && !rt.isBlank()) {
            tokenService.logoutByRefreshToken(rt);
        }

        AuthCookieUtil.deleteCookie(res, "AT", null, "/", SECURE_COOKIE, "Lax");
        AuthCookieUtil.deleteCookie(res, "RT", null, "/", SECURE_COOKIE, "Lax");
        AuthCookieUtil.deleteCookie(res, "FAM", null, "/", SECURE_COOKIE, "Lax");

        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }

    /**
     * 자격증명 검증
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
     * 이메일 중복 여부 확인
     */
    @Transactional
    public boolean isEmailDuplicated(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 이메일 인증코드 발송
     */
    public void sendVerificationCode(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        emailStore.save(email, code, Duration.ofMinutes(10));

        try {
            emailService.sendVerificationCode(email, code);
        } catch (MailException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "인증 메일 전송 중 오류가 발생했습니다.");
        }
    }

    /**
     * 이메일 인증코드 검증
     */
    public boolean verifyCode(String email, String code) {
        return emailStore.checkAndMarkVerified(email, code);
    }

    /**
     * 비밀번호 재설정
     */
    @Transactional
    public void resetPassword(ResetPasswordRequestDto req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }
        if (!emailStore.isVerified(req.getEmail())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이메일 인증이 필요합니다.");
        }

        Users user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "이메일에 해당하는 사용자를 찾을 수 없습니다."));

        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        userRepository.save(user);
    }

    /**
     * 내 프로필 조회
     */
    @Transactional(readOnly = true)
    public MeProfileResponseDto getMeProfile(Users user) {
        Users u = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return MeProfileResponseDto.of(u);
    }

    /**
     * 회원탈퇴
     */
    @Transactional
    public void withdraw(Users user, String password, HttpServletRequest request, HttpServletResponse response) {
        String loginType = user.getLoginType();

        if ("normal".equals(loginType)) {
            if (password == null || password.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "현재 비밀번호가 필요합니다.");
            }
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
            }
        } else if (!"google".equals(loginType)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 로그인 타입입니다.");
        }

        logout(request, response);
        softDeleteUser(user);
    }

    private void softDeleteUser(Users user) {
        String anonymizedEmail = "deleted-" + user.getUserId() + "-" + user.getEmail();
        user.setEmail(anonymizedEmail);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }
}
