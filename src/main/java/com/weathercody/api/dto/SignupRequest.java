package com.weathercody.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String email;
    private String password;
    private String name;
    private String gender;
    private String phone;
    private Short heightCm;
    private Short weightKg;
    private Short footSizeMm;
}