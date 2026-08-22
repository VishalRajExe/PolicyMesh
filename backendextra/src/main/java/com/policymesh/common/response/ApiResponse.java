package com.policymesh.common.response;

/**
 * Lightweight generic success wrapper, used where a plain DTO isn't
 * expressive enough on its own (e.g. simple message-only endpoints).
 */
public record ApiResponse<T>(boolean success, T data, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }
}
