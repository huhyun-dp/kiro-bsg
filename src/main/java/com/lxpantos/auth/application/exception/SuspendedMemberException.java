package com.lxpantos.auth.application.exception;

/**
 * 정지(SUSPENDED)된 회원이 로그인을 시도할 때 발생한다.
 */
public class SuspendedMemberException extends RuntimeException {
    public SuspendedMemberException() {
        super("정지된 계정입니다. 관리자에게 문의해 주세요.");
    }
}
