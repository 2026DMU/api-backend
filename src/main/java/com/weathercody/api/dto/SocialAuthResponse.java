package com.weathercody.api.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SocialAuthResponse {

    private SocialAuthAction action;
    private UUID userId;
    private String accessToken;
    private String provider;
    private String email;
    private String name;
    // 소셜 프로필만으로 계정을 만들 수 없을 때만 추가 입력 항목을 내려줍니다.
    private List<String> requiredFields;

    public static SocialAuthResponse login(UUID userId, String accessToken, String provider, String email, String name) {
        return SocialAuthResponse.builder()
                .action(SocialAuthAction.LOGIN)
                .userId(userId)
                .accessToken(accessToken)
                .provider(provider)
                .email(email)
                .name(name)
                .requiredFields(List.of())
                .build();
    }

    public static SocialAuthResponse signupRequired(
            String provider,
            String email,
            String name,
            List<String> requiredFields
    ) {
        return SocialAuthResponse.builder()
                .action(SocialAuthAction.SIGNUP_REQUIRED)
                .provider(provider)
                .email(email)
                .name(name)
                .requiredFields(List.copyOf(requiredFields))
                .build();
    }

    public static SocialAuthResponse signupCompleted(
            UUID userId,
            String accessToken,
            String provider,
            String email,
            String name
    ) {
        return SocialAuthResponse.builder()
                .action(SocialAuthAction.SIGNUP_COMPLETED)
                .userId(userId)
                .accessToken(accessToken)
                .provider(provider)
                .email(email)
                .name(name)
                .requiredFields(List.of())
                .build();
    }
}
