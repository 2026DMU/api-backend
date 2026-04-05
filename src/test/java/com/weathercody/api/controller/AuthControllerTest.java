package com.weathercody.api.controller;

import com.weathercody.api.config.SecurityConfig;
import com.weathercody.api.dto.SocialAuthResponse;
import com.weathercody.api.dto.TokenResponse;
import com.weathercody.api.filter.LoggingFilter;
import com.weathercody.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({LoggingFilter.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("signup wraps the response in ApiResponse")
    void signup_responseIsWrappedInApiResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        given(authService.signup(any())).willReturn(userId);

        String requestBody = """
                {
                  "email": "test@example.com",
                  "password": "password123",
                  "name": "Kim",
                  "gender": "F",
                  "phone": "010-1111-2222",
                  "heightCm": 165,
                  "weightKg": 55,
                  "footSizeMm": 240
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("환영합니다! Kim 님, 회원가입이 완료되었습니다. 날씨에 맞는 코디를 추천 받아보세요!"))
                .andExpect(jsonPath("$.data").value(userId.toString()));
    }

    @Test
    @DisplayName("login wraps the response in ApiResponse")
    void login_responseIsWrappedInApiResponse() throws Exception {
        given(authService.login(any())).willReturn(new TokenResponse("mock-jwt-token"));

        String requestBody = """
                {
                  "email": "test@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("social auth wraps the signup-required response in ApiResponse")
    void socialAuth_responseIsWrappedInApiResponse() throws Exception {
        given(authService.socialAuthenticate(any())).willReturn(
                SocialAuthResponse.signupRequired("GOOGLE", "social@example.com", "Social User", List.of("gender", "birthDate", "phone"))
        );

        String requestBody = """
                {
                  "provider": "GOOGLE",
                  "providerUserId": "google-123",
                  "email": "social@example.com",
                  "name": "Social User"
                }
                """;

        mockMvc.perform(post("/api/auth/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("추가 회원 정보 입력이 필요합니다."))
                .andExpect(jsonPath("$.data.action").value("SIGNUP_REQUIRED"))
                .andExpect(jsonPath("$.data.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.data.email").value("social@example.com"))
                .andExpect(jsonPath("$.data.requiredFields[0]").value("gender"));
    }
}
