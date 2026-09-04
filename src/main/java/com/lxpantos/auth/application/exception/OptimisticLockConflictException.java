package com.lxpantos.auth.application.exception;

/**
 * 동시에 다른 관리자가 같은 회원을 변경해 version 이 일치하지 않을 때 발생한다.
 */
public class OptimisticLockConflictException extends RuntimeException {
    public OptimisticLockConflictException() {
        super("다른 관리자가 먼저 변경했습니다. 최신 정보를 다시 조회해 주세요.");
    }
}
