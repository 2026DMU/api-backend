package com.weathercody.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        filterChain.doFilter(requestWrapper, responseWrapper);
        long duration = System.currentTimeMillis() - startTime;

        String requestBody = maskPassword(new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8));
        String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

        // 회원가입 요청 예시:
        // [REQUEST]  POST /api/auth/signup | body: {"email":"test@example.com","password":"[MASKED]","name":"홍길동",...}
        // 회원가입 응답 예시:
        // [RESPONSE] 200 | 23ms | body: {"data":"환영합니다! 홍길동 님...","statusCode":200,"message":"회원가입이 완료되었습니다."}
        log.info("[REQUEST]  {} {} | body: {}", request.getMethod(), request.getRequestURI(), requestBody);
        log.info("[RESPONSE] {} | {}ms | body: {}", response.getStatus(), duration, responseBody);

        responseWrapper.copyBodyToResponse();
    }

    private String maskPassword(String body) {
        if (body == null || body.isBlank()) return body;
        return body.replaceAll("(\"password\"\\s*:\\s*\")([^\"]*)(\")", "$1[MASKED]$3");
    }
}
