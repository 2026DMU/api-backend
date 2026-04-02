package com.weathercody.api.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingFilterTest {

    private Method maskPasswordMethod;

    @BeforeEach
    void setUp() throws Exception {
        maskPasswordMethod = LoggingFilter.class.getDeclaredMethod("maskPassword", String.class);
        maskPasswordMethod.setAccessible(true);
    }

    private String maskPassword(String body) throws Exception {
        return (String) maskPasswordMethod.invoke(new LoggingFilter(), body);
    }

    @Test
    @DisplayName("password 필드 값이 [MASKED] 로 치환된다")
    void maskPassword_replacesValueWithMasked() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"password\":\"secret123\",\"name\":\"홍길동\"}";

        String result = maskPassword(body);

        assertThat(result).contains("[MASKED]");
        assertThat(result).doesNotContain("secret123");
    }

    @Test
    @DisplayName("password 외 필드는 마스킹되지 않는다")
    void maskPassword_preservesOtherFields() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"password\":\"mypassword\"}";

        String result = maskPassword(body);

        assertThat(result).contains("test@example.com");
        assertThat(result).contains("[MASKED]");
    }

    @Test
    @DisplayName("빈 문자열은 그대로 반환된다")
    void maskPassword_returnsBlankAsIs() throws Exception {
        assertThat(maskPassword("")).isBlank();
    }

    @Test
    @DisplayName("null 은 그대로 반환된다")
    void maskPassword_returnsNullAsIs() throws Exception {
        assertThat(maskPassword(null)).isNull();
    }

    @Test
    @DisplayName("password 필드가 없으면 body 가 변경되지 않는다")
    void maskPassword_noChangeWhenNoPasswordField() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"name\":\"홍길동\"}";

        String result = maskPassword(body);

        assertThat(result).isEqualTo(body);
    }

    @Test
    @DisplayName("password 값이 빈 문자열이어도 마스킹된다")
    void maskPassword_masksEmptyPasswordValue() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"password\":\"\"}";

        String result = maskPassword(body);

        assertThat(result).contains("\"password\":\"[MASKED]\"");
    }
}
