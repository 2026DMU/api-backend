package com.weathercody.api.common;

import com.weathercody.api.dto.common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("success() - data, statusCode, message 가 올바르게 설정된다")
    void success_setsAllFields() {
        ApiResponse<String> response = ApiResponse.success("결과 데이터", 200, "요청 성공");

        assertThat(response.getData()).isEqualTo("결과 데이터");
        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("요청 성공");
    }

    @Test
    @DisplayName("success() - data 가 null 이어도 정상 생성된다")
    void success_withNullData() {
        ApiResponse<String> response = ApiResponse.success(null, 200, "데이터 없음");

        assertThat(response.getData()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("error() - data 는 null, statusCode 와 message 가 설정된다")
    void error_returnsNullData() {
        ApiResponse<Object> response = ApiResponse.error(400, "잘못된 요청");

        assertThat(response.getData()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.getMessage()).isEqualTo("잘못된 요청");
    }

    @Test
    @DisplayName("success() - 제네릭 타입에 관계없이 동작한다")
    void success_worksWithDifferentTypes() {
        ApiResponse<Integer> intResponse = ApiResponse.success(42, 200, "정수 반환");
        ApiResponse<Boolean> boolResponse = ApiResponse.success(true, 200, "불린 반환");

        assertThat(intResponse.getData()).isEqualTo(42);
        assertThat(boolResponse.getData()).isTrue();
    }
}
