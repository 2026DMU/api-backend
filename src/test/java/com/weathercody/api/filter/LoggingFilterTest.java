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
    @DisplayName("maskPassword replaces the password value")
    void maskPassword_replacesValueWithMasked() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"password\":\"secret123\",\"name\":\"Kim\"}";

        String result = maskPassword(body);

        assertThat(result).contains("[MASKED]");
        assertThat(result).doesNotContain("secret123");
    }

    @Test
    @DisplayName("maskPassword preserves other fields")
    void maskPassword_preservesOtherFields() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"password\":\"mypassword\"}";

        String result = maskPassword(body);

        assertThat(result).contains("test@example.com");
        assertThat(result).contains("[MASKED]");
    }

    @Test
    @DisplayName("maskPassword returns blank input as-is")
    void maskPassword_returnsBlankAsIs() throws Exception {
        assertThat(maskPassword("")).isBlank();
    }

    @Test
    @DisplayName("maskPassword returns null as-is")
    void maskPassword_returnsNullAsIs() throws Exception {
        assertThat(maskPassword(null)).isNull();
    }

    @Test
    @DisplayName("maskPassword does not change payloads without a password field")
    void maskPassword_noChangeWhenNoPasswordField() throws Exception {
        String body = "{\"email\":\"test@example.com\",\"name\":\"Kim\"}";

        String result = maskPassword(body);

        assertThat(result).isEqualTo(body);
    }
}
