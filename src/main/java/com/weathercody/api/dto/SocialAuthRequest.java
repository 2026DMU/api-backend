package com.weathercody.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SocialAuthRequest {
    private String provider;
    private String providerUserId;
    private String email;
    private String name;
    private String gender;
    private String phone;
    private LocalDate birthDate;
    private Short heightCm;
    private Short weightKg;
    private Short footSizeMm;
    private String profileImageUrl;
}
