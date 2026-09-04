package com.lxpantos.auth.application.exception;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException() {
        super("대상 회원을 찾을 수 없습니다.");
    }
}
