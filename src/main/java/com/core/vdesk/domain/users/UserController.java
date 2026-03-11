package com.core.vdesk.domain.users;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.core.vdesk.domain.emails.EmailRequestDto;
import com.core.vdesk.domain.emails.EmailVerifyRequestDto;
import com.core.vdesk.domain.users.dto.LoginRequestDto;
import com.core.vdesk.domain.users.dto.MeProfileResponseDto;
import com.core.vdesk.domain.users.dto.ResetPasswordRequestDto;
import com.core.vdesk.domain.users.dto.SignupRequestDto;
import com.core.vdesk.domain.users.dto.WithdrawRequestDto;
import com.core.vdesk.global.oauth2.PrincipalDetails;
import com.core.vdesk.global.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 일반로그인 로그인 컨트롤러
     * @req 이메일, 패스워드
     * @return 쿠키에 JWT 전달
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody @Valid LoginRequestDto req, HttpServletResponse res) {

        userService.login(req, res);
        return ResponseEntity.ok(ApiResponse.ok("로그인 성공", null));
    }

    /**
     * 일반로그인 회원가입 컨트롤러
     * @req 이메일, 이메일 인증, 패스워드, 패스워드 확인
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<?>> signup(@RequestBody @Valid SignupRequestDto req){
        userService.signup(req);
        return ResponseEntity.ok(ApiResponse.ok("회원가입 성공", null));
    }

    /**
     * 이메일 중복확인 컨트롤러
     * @req 이메일
     * @return boolean
     */
    @PostMapping("/duplications/email")
    public ResponseEntity<ApiResponse<Boolean>> dupEmail(@RequestBody @Valid EmailRequestDto req){
        boolean result = userService.isEmailDuplicated(req.getEmail());
        String message = result ? "이미 사용중인 이메일입니다." : "사용가능 이메일입니다.";

        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }

    /**
     * 이메일 인증코드 요청 컨트롤러
     * req 이메일
     * @return 이메일로 코드 발송
     */
    @PostMapping("/email/verification")
    public ResponseEntity<ApiResponse<?>> send(@RequestBody @Valid EmailRequestDto req) {
        userService.sendVerificationCode(req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("이메일 코드 발송 성공", null));
    }

    /**
     * 이메일 인증코드 확인 컨트롤러
     * @req 이메일
     * @return boolean
     */
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Boolean>> verify(@RequestBody @Valid EmailVerifyRequestDto req){
        boolean result = userService.verifyCode(req.getEmail(), req.getCode());
        String message = result ? "인증을 성공했습니다." : "코드가 일치하지 않습니다.";
        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeProfileResponseDto>> me(@AuthenticationPrincipal PrincipalDetails principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Users user = principal.getUser();
        MeProfileResponseDto result = userService.getMeProfile(user);

        return ResponseEntity.ok(ApiResponse.ok("로그인 사용자 정보", result));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest req, HttpServletResponse res) {


        userService.logout(req, res);

        return ResponseEntity.ok(ApiResponse.ok("로그아웃 완료", null));
    }

    // 비밀번호 변경
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<?>> resetPassword(@RequestBody @Valid ResetPasswordRequestDto req
                                                             ) {

        // 인증된 사용자 이메일 기반으로 비밀번호 변경
        userService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호 변경 성공", null));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<?>> withdraw(@AuthenticationPrincipal PrincipalDetails principal,@RequestBody WithdrawRequestDto req, HttpServletRequest request,
                                                   HttpServletResponse response) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        Users user = principal.getUser();
        String password = (req != null ? req.getPassword(): null);

        userService.withdraw(user, password, request, response);

        return ResponseEntity.ok(ApiResponse.ok("회원탈퇴 완료", null));
    }


}