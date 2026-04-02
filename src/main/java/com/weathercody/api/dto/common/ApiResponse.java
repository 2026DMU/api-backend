package com.weathercody.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private T data;
    private int statusCode;
    private String message;

    public static <T> ApiResponse<T> success(T data, int statusCode, String message) {
        return new ApiResponse<>(data, statusCode, message);
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return new ApiResponse<>(null, statusCode, message);
    }
}
