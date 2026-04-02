package com.weathercody.api.controller;

import com.weathercody.api.dto.TokenResponse;
import com.weathercody.api.config.SecurityConfig;
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

import static org.hamcrest.Matchers.containsString;
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
    @DisplayName("회원가입 응답이 ApiResponse 형식(data, statusCode, message)으로 반환된다")
    void signup_responseIsWrappedInApiResponse() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123",
                    "name": "홍길동"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data").value(containsString("홍길동")));
    }

    @Test
    @DisplayName("로그인 응답이 ApiResponse 형식(data.accessToken, statusCode, message)으로 반환된다")
    void login_responseIsWrappedInApiResponse() throws Exception {
        String requestBody = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

        given(authService.login(any())).willReturn(new TokenResponse("mock-jwt-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken").value("mock-jwt-token"));
    }
}
