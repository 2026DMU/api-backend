package com.weathercody.api.controller;

import com.weathercody.api.dto.LoginRequest;
import com.weathercody.api.dto.SignupRequest;
import com.weathercody.api.dto.TokenResponse;
import com.weathercody.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        authService.signup(request);

        String welcomeMessage = String.format(
                "환영합니다!\n%s 님, 회원가입이 완료되었습니다.\n날씨에 맞는 코디를 추천 받아보세요!",
                request.getName()
        );

        return ResponseEntity.ok(welcomeMessage);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}