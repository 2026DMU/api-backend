package com.weathercody.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SocialAuthResponse {

    private UUID userId;
    private String accessToken;
    private String provider;
    private String email;
    private String name;
    // 소셜 로그인은 가입/로그인을 우선 완료하고,
    // 이 값으로만 "추가 프로필 입력이 필요한지" 프론트에서 판단합니다.
    private boolean profileCompleted;

    public static SocialAuthResponse authenticated(
            UUID userId,
            String accessToken,
            String provider,
            String email,
            String name,
            boolean profileCompleted
    ) {
        // 기존 연동 계정 로그인과 첫 소셜 회원가입 완료 응답을 같은 형태로 맞춥니다.
        return SocialAuthResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .provider(provider)
                .email(email)
                .name(name)
                .profileCompleted(profileCompleted)
                .build();
    }
}
