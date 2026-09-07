package com.lxpantos.auth.domain.member;

/**
 * 회원 계정 상태. SUSPENDED 회원은 로그인 및 인증된 요청을 수행할 수 없다.
 */
public enum MemberStatus {
    ACTIVE,
    SUSPENDED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
