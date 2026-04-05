package com.weathercody.api.controller;

import com.weathercody.api.dto.LoginRequest;
import com.weathercody.api.dto.SignupRequest;
import com.weathercody.api.dto.SocialAuthAction;
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
        // 하나의 엔드포인트에서 기존 로그인, 추가 정보 입력 필요,
        // 소셜 회원가입 완료까지 모두 분기합니다.
        SocialAuthResponse response = authService.socialAuthenticate(request);
        return ResponseEntity.ok(ApiResponse.success(response, 200, resolveSocialMessage(response.getAction())));
    }

    private String resolveSocialMessage(SocialAuthAction action) {
        return switch (action) {
            case LOGIN -> "소셜 로그인이 완료되었습니다.";
            case SIGNUP_REQUIRED -> "추가 회원 정보 입력이 필요합니다.";
            case SIGNUP_COMPLETED -> "소셜 회원가입이 완료되었습니다.";
        };
    }
}
