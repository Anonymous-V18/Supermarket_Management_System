package com.anonymous.exception;

import com.anonymous.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<ErrorCode>> handleAppException(AppException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.<ErrorCode>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorCode>> handleUncategorizedException(RuntimeException e) {
        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.<ErrorCode>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorCode>> handleAccessDeniedException(AccessDeniedException e) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.<ErrorCode>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorCode>> handleValidationParamsException(MethodArgumentNotValidException e) {
        String key = Objects.requireNonNull(e.getFieldError()).getField();
        ErrorCode errorCode = ErrorCode.INVALID_KEY;

        try {
            errorCode = ErrorCode.valueOf(key);
        } catch (IllegalArgumentException ignored) {

        }

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(ApiResponse.<ErrorCode>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

}
