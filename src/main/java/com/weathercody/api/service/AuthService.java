package com.weathercody.api.service;

import com.weathercody.api.dto.LoginRequest;
import com.weathercody.api.dto.SignupRequest;
import com.weathercody.api.dto.TokenResponse;
import com.weathercody.api.entity.User;
import com.weathercody.api.repository.UserRepository;
import com.weathercody.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입 로그 흐름 예시:
    //
    // [성공]
    // INFO  LoggingFilter - [REQUEST]  POST /api/auth/signup | body: {"email":"test@example.com","password":"[MASKED]","name":"홍길동",...}
    // INFO  AuthService   - 회원가입 성공 - email: test@example.com
    // INFO  LoggingFilter - [RESPONSE] 200 | 23ms | body: {"data":"환영합니다! 홍길동 님...","statusCode":200,"message":"회원가입이 완료되었습니다."}
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

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치: {}", request.getEmail());
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtTokenProvider.createToken(user.getEmail());
        log.info("로그인 성공 - email: {}", request.getEmail());
        return new TokenResponse(token);
    }
}