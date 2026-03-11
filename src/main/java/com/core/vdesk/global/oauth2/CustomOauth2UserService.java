package com.core.vdesk.global.oauth2;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.core.vdesk.domain.users.UserRepository;
import com.core.vdesk.domain.users.Users;
import com.core.vdesk.global.exception.BusinessException;
import com.core.vdesk.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * OAuth2 로그인 과정에서 사용자 정보를 조회하고,
     * 해당 정보를 바탕으로 내부 Users 엔티티를 생성하거나 기존 회원을 조회하여
     * Spring Security 인증 객체(PrincipalDetails)로 반환하는 서비스.
     * 핵심 역할:
     *   - OAuth2 Provider(Google, Kakao 등)에서 사용자 프로필 정보 수신
     *   - provider별 attribute 파싱 → Oauth2UserInfo 객체로 표준화
     *   - 이메일 기반으로 신규 가입 또는 기존 사용자 로그인 처리
     *   - 일반 로그인 사용자(normal)는 소셜 로그인으로 진입 불가(보안·정합성)
     * 인증 흐름:
     *   OAuth2 로그인 성공 → loadUser() 실행 → provider 사용자정보 파싱 →
     *   saveOrLogin() → PrincipalDetails 반환 → Oauth2SuccessHandler에서 JWT 발급
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("CustomOauth2UserService 유저 정보 저장 시작");

        try {

            //등록된 클라이언트가 구글인지 확인
            String requestId = userRequest.getClientRegistration().getRegistrationId();  // 요청한 oath2 사이트 회사명
            if (!"google".equals(requestId)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "구글 제공자가 아닙니다.");
            }

            // 구글 사용자 정보 조회
            OAuth2User user = super.loadUser(userRequest);
            Map<String, Object> attributes = user.getAttributes();

            Oauth2UserInfo oauth2UserInfo = new GoogleUserInfo(attributes);
            Users u = saveOrLogin(oauth2UserInfo.getId(), oauth2UserInfo.getEmail(), oauth2UserInfo.getName(), requestId);

            return new PrincipalDetails(u);

        } catch (BusinessException e) {
            // BusinessException → OAuth2AuthenticationException 변환
            OAuth2Error error = new OAuth2Error(
                    e.getErrorCode().name(),      // error code
                    e.getMessage(),               // description
                    null
            );
            throw new OAuth2AuthenticationException(error, e.getMessage(), e);

        } catch (Exception e) {
            // 기타 예상치 못한 예외 처리
            OAuth2Error error = new OAuth2Error(
                    "oauth2_internal_error",
                    "소셜 로그인 처리 중 알 수 없는 오류가 발생했습니다.",
                    null
            );
            throw new OAuth2AuthenticationException(error, error.getDescription(), e);
        }
    }

    /**
     * 규칙
     * 1) email이 DB에 있으면
     *    - loginType == "normal"  -> 소셜 로그인 금지(에러)
     *    - loginType == "google" -> 기존 계정으로 로그인 처리(그대로 리턴)
     * 2) email이 없으면 신규 가입
     */
    private Users saveOrLogin(String id, String email, String name,  String requestId) {

        return userRepository.findByEmail(email)
                // deleted = true 인 탈퇴 계정은 조회 제외
                .filter(existing -> !existing.isDeleted())
                .map(existing -> {
                    // 기존 회원 존재
                    if ("normal".equalsIgnoreCase(existing.getLoginType())) {
                        // 일반 로그인 회원이면 소셜 로그인 금지
                        throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 가입된 이메일입니다. 일반 로그인 방식을 사용하세요");
                    }
                    if ("google".equalsIgnoreCase(existing.getLoginType())) {
                        // 이미 구글 로그인 회원이면 그대로 로그인
                        return existing;
                    }
                    if ("kakao".equalsIgnoreCase(existing.getLoginType())) {
                        // 이미 구글 로그인 회원이면 그대로 로그인
                        return existing;
                    }
                    if ("naver".equalsIgnoreCase(existing.getLoginType())) {
                        // 이미 구글 로그인 회원이면 그대로 로그인
                        return existing;
                    }
                    // 혹시 다른 값이 있을 때 (방어적 처리)
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 이메일은 소셜 로그인으로 가입되어 있지 않습니다."
                    );
                })
                .orElseGet(() -> {
                    // 신규 가입
                    Users u = new Users();
                    u.setEmail(email);
                    u.setLoginType(requestId);
                    u.setUserName(name);
                    u.setRoles("ROLE_USER");
                    u.setPasswordHash(passwordEncoder.encode("google:" + id));
                    u.setDeleted(false);
                    u.setDeletedAt(null);
                    Users savedUser = userRepository.save(u);

                    return savedUser;
                });
    }
}
