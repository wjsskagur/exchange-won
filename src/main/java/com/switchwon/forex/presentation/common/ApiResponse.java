package com.switchwon.forex.presentation.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 공통 API 응답 래퍼
 *
 * 과제 명세 기준:
 * - 성공: code="OK", message="SUCCESS"
 * - 에러: code=에러코드, message=에러메시지
 *
 * @JsonInclude(NON_NULL): returnObject가 null인 에러 응답에서 해당 필드 생략
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    String code,
    String message,
    T returnObject
) {
    private static final String SUCCESS_CODE = "OK";
    private static final String SUCCESS_MESSAGE = "SUCCESS";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
