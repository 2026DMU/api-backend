package com.weathercody.api.service;

import com.weathercody.api.dto.LoginRequest;
import com.weathercody.api.dto.SignupRequest;
import com.weathercody.api.dto.SocialAuthRequest;
import com.weathercody.api.dto.SocialAuthResponse;
import com.weathercody.api.dto.TokenResponse;
import com.weathercody.api.entity.SocialAccount;
import com.weathercody.api.entity.User;
import com.weathercody.api.repository.SocialAccountRepository;
import com.weathercody.api.repository.UserRepository;
import com.weathercody.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // provider 값은 한 번만 정규화하고, 이후 로직은 provider 종류와 무관하게 공통 처리합니다.
    private static final List<String> SUPPORTED_SOCIAL_PROVIDERS = List.of("GOOGLE", "KAKAO", "NAVER");

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입 로그 흐름 예시:
    //
    // [성공]
    // INFO  LoggingFilter - [REQUEST]  POST /api/auth/signup | body: {"email":"test@example.com","password":"[MASKED]","name":"홍길동",...}
    // INFO  AuthService   - 회원가입 성공 - email: test@example.com
    // INFO  LoggingFilter - [RESPONSE] 200 | 23ms | body: {"data":"UUID","statusCode":200,"message":"회원가입이 완료되었습니다."}
    //
    // [실패 - 중복 이메일]
    // INFO  LoggingFilter - [REQUEST]  POST /api/auth/signup | body: {"email":"test@example.com","password":"[MASKED]","name":"홍길동",...}
    // WARN  AuthService   - 회원가입 실패 - 이미 존재하는 이메일: test@example.com
    // INFO  LoggingFilter - [RESPONSE] 500 | 12ms | body: ...
    @Transactional
    public UUID signup(SignupRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호가 존재하지 않습니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("회원가입 실패 - 이미 존재하는 이메일: {}", request.getEmail());
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .gender(request.getGender())
                .phone(request.getPhone())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .footSizeMm(request.getFootSizeMm())
                .build();

        userRepository.save(user);
        log.info("회원가입 성공 - email: {}", request.getEmail());
        return user.getId();
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호가 존재하지 않습니다.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 가입되지 않은 이메일: {}", request.getEmail());
                    return new RuntimeException("가입되지 않은 이메일입니다.");
                });

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            log.warn("로그인 실패 - 소셜 계정입니다. email: {}", request.getEmail());
            throw new RuntimeException("소셜 로그인을 이용해주세요.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치: {}", request.getEmail());
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtTokenProvider.createToken(user.getEmail());
        log.info("로그인 성공 - email: {}", request.getEmail());
        return new TokenResponse(token);
    }

    @Transactional
    public SocialAuthResponse socialAuthenticate(SocialAuthRequest request) {
        String provider = normalizeProvider(request.getProvider());
        String providerUserId = trimToNull(request.getProviderUserId());

        if (providerUserId == null) {
            throw new IllegalArgumentException("providerUserId가 존재하지 않습니다.");
        }

        Optional<SocialAccount> existingSocialAccount =
                socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (existingSocialAccount.isPresent()) {
            // 이미 연동된 소셜 계정이면 바로 JWT를 발급하고 로그인 처리를 끝냅니다.
            // 이 경우에는 추가 정보 입력 여부와 상관없이 로그인은 성공시키고,
            // profileCompleted 값으로만 이후 보완 유도를 판단합니다.
            User user = existingSocialAccount.get().getUser();
            log.info("소셜 로그인 성공 - provider: {}, email: {}", provider, user.getEmail());
            return SocialAuthResponse.authenticated(
                    user.getId(),
                    jwtTokenProvider.createToken(user.getEmail()),
                    provider,
                    user.getEmail(),
                    user.getName(),
                    isProfileCompleted(user)
            );
        }

        String email = trimToNull(request.getEmail());
        String name = trimToNull(request.getName());

        if (email != null && userRepository.existsByEmail(email)) {
            // 문서 기준으로는 기존 이메일 계정을 자동 연동하지 않습니다.
            // 사용자가 이미 이메일 회원가입을 했다면 기존 로그인 경로를 안내합니다.
            log.warn("소셜 로그인 실패 - 이미 가입된 이메일: {}", email);
            throw new RuntimeException("이미 가입된 이메일입니다. 기존 로그인을 이용해주세요.");
        }

        if (email == null || name == null) {
            // 이메일과 이름은 우리 서비스 계정을 생성하는 최소 기준 정보입니다.
            // 소셜 제공자에서 이 값조차 내려주지 않으면 회원 생성 자체를 진행할 수 없습니다.
            throw new IllegalArgumentException("소셜 회원가입에 필요한 기본 정보가 부족합니다.");
        }

        String phone = trimToNull(request.getPhone());
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new RuntimeException("이미 존재하는 전화번호입니다.");
        }

        User user = User.builder()
                .email(email)
                // 소셜로 처음 가입한 계정은 아직 로컬 비밀번호가 없습니다.
                .passwordHash(null)
                .name(name)
                .gender(trimToNull(request.getGender()))
                .phone(phone)
                .birthDate(request.getBirthDate())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .footSizeMm(request.getFootSizeMm())
                .profileImageUrl(trimToNull(request.getProfileImageUrl()))
                .build();

        userRepository.save(user);
        socialAccountRepository.save(SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(user.getEmail())
                .build());

        // 첫 소셜 로그인도 가능한 한 즉시 회원가입 + 로그인까지 완료합니다.
        // 부족한 프로필 정보는 profileCompleted=false 로 내려서
        // 메인 진입 후 마이페이지에서 보완하도록 유도합니다.
        log.info("소셜 회원가입 성공 - provider: {}, email: {}", provider, user.getEmail());
        return SocialAuthResponse.authenticated(
                user.getId(),
                jwtTokenProvider.createToken(user.getEmail()),
                provider,
                user.getEmail(),
                user.getName(),
                isProfileCompleted(user)
        );
    }

    private String normalizeProvider(String provider) {
        String normalizedProvider = trimToNull(provider);
        if (normalizedProvider == null) {
            throw new IllegalArgumentException("provider가 존재하지 않습니다.");
        }

        normalizedProvider = normalizedProvider.toUpperCase(Locale.ROOT);
        if (!SUPPORTED_SOCIAL_PROVIDERS.contains(normalizedProvider)) {
            throw new IllegalArgumentException("지원되지 않는 provider 입니다.");
        }

        return normalizedProvider;
    }

    private boolean isProfileCompleted(User user) {
        // 현재 기준으로는 가입 후 반드시 보완을 유도할 핵심 항목만 체크합니다.
        // 키/몸무게/발 사이즈는 추천 품질에는 중요하지만, 로그인 차단 조건으로는 보지 않습니다.
        return trimToNull(user.getGender()) != null
                && user.getBirthDate() != null
                && trimToNull(user.getPhone()) != null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
