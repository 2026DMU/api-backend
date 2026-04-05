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

import java.util.ArrayList;
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

    @Transactional
    public UUID signup(SignupRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호가 존재하지 않습니다.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("회원가입 실패 - 이미 존재하는 이메일 {}", request.getEmail());
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
                    log.warn("로그인 실패 - 가입되지 않은 이메일 {}", request.getEmail());
                    return new RuntimeException("가입되지 않은 이메일입니다.");
                });

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            log.warn("로그인 실패 - 소셜 계정입니다. email: {}", request.getEmail());
            throw new RuntimeException("소셜 로그인을 이용해주세요.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치 {}", request.getEmail());
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
            User user = existingSocialAccount.get().getUser();
            log.info("소셜 로그인 성공 - provider: {}, email: {}", provider, user.getEmail());
            return SocialAuthResponse.login(
                    user.getId(),
                    jwtTokenProvider.createToken(user.getEmail()),
                    provider,
                    user.getEmail(),
                    user.getName()
            );
        }

        String email = trimToNull(request.getEmail());
        String name = trimToNull(request.getName());

        if (email != null && userRepository.existsByEmail(email)) {
            log.warn("소셜 로그인 실패 - 이미 가입된 이메일 {}", email);
            throw new RuntimeException("이미 가입된 이메일입니다. 기존 로그인을 이용해주세요.");
        }

        List<String> missingFields = getMissingSignupFields(request);
        if (!missingFields.isEmpty()) {
            // 앱은 이 목록을 보고 추가 정보 입력 화면에서 무엇을 더 받아야 하는지 결정합니다.
            log.info("소셜 회원가입 추가 정보 필요 - provider: {}, providerUserId: {}", provider, providerUserId);
            return SocialAuthResponse.signupRequired(provider, email, name, missingFields);
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

        log.info("소셜 회원가입 성공 - provider: {}, email: {}", provider, user.getEmail());
        return SocialAuthResponse.signupCompleted(
                user.getId(),
                jwtTokenProvider.createToken(user.getEmail()),
                provider,
                user.getEmail(),
                user.getName()
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

    private List<String> getMissingSignupFields(SocialAuthRequest request) {
        // 현재 앱 회원가입 화면 기준으로 필수 입력으로 보는 항목들입니다.
        List<String> missingFields = new ArrayList<>();

        if (trimToNull(request.getEmail()) == null) {
            missingFields.add("email");
        }
        if (trimToNull(request.getName()) == null) {
            missingFields.add("name");
        }
        if (trimToNull(request.getGender()) == null) {
            missingFields.add("gender");
        }
        if (request.getBirthDate() == null) {
            missingFields.add("birthDate");
        }
        if (trimToNull(request.getPhone()) == null) {
            missingFields.add("phone");
        }

        return missingFields;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
