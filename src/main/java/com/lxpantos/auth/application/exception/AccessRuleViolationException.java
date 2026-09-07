package com.lxpantos.auth.application.exception;

/**
 * 권한 변경 시 비즈니스 규칙(자기 강등/정지 금지, 마지막 관리자 보호 등)을 위반할 때 발생한다.
 * 서버 측 검증 실패이므로 HTTP 400 으로 매핑한다.
 */
public class AccessRuleViolationException extends RuntimeException {
    public AccessRuleViolationException(String message) {
        super(message);
    }
}
