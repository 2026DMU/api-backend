package com.weathercody.api.controller;

import com.weathercody.api.dto.LoginRequest;
import com.weathercody.api.dto.SignupRequest;
import com.weathercody.api.dto.SocialAuthRequest;
import com.weathercody.api.dto.SocialAuthResponse;
import com.weathercody.api.dto.TokenResponse;
import com.weathercody.api.dto.common.ApiResponse;
import com.weathercody.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UUID>> signup(@RequestBody SignupRequest request) {
        UUID userId = authService.signup(request);
        String welcomeMessage = String.format(
                "환영합니다! %s 님, 회원가입이 완료되었습니다. 날씨에 맞는 코디를 추천 받아보세요!",
                request.getName()
        );

        return ResponseEntity.ok(ApiResponse.success(userId, 200, welcomeMessage));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse, 200, "로그인에 성공했습니다."));
    }

    @PostMapping("/social")
    public ResponseEntity<ApiResponse<SocialAuthResponse>> socialAuthenticate(@RequestBody SocialAuthRequest request) {
        // 소셜 로그인은 가능한 한 바로 가입/로그인까지 완료합니다.
        // 추가 정보가 비어 있더라도 응답의 profileCompleted 값으로만 보완 여부를 안내합니다.
        // 프론트는 로그인 성공 후 메인으로 진입시키고, profileCompleted=false 이면
        // 마이페이지 또는 프로필 보완 화면으로 유도하면 됩니다.
        SocialAuthResponse response = authService.socialAuthenticate(request);
        return ResponseEntity.ok(ApiResponse.success(response, 200, resolveSocialMessage(response.isProfileCompleted())));
    }

    private String resolveSocialMessage(boolean profileCompleted) {
        return profileCompleted
                ? "소셜 로그인이 완료되었습니다."
                : "소셜 로그인이 완료되었습니다. 추가 정보는 마이페이지에서 입력해주세요.";
    }
}
