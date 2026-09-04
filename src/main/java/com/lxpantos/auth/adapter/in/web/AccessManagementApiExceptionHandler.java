package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.application.exception.AccessRuleViolationException;
import com.lxpantos.auth.application.exception.MemberNotFoundException;
import com.lxpantos.auth.application.exception.OptimisticLockConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 관리자 API 전용 예외 → HTTP 상태/한국어 JSON 매핑.
 * 스택 트레이스나 내부 메시지는 노출하지 않는다.
 */
@RestControllerAdvice(assignableTypes = AccessManagementApiController.class)
public class AccessManagementApiExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(MemberNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(OptimisticLockConflictException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(AccessRuleViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleRuleViolation(AccessRuleViolationException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError != null && fieldError.getDefaultMessage() != null
                ? fieldError.getDefaultMessage()
                : "입력값이 올바르지 않습니다.";
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformed(Exception exception) {
        return build(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), message));
    }
}
