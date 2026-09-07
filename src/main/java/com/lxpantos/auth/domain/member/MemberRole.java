package com.lxpantos.auth.domain.member;

/**
 * 회원 역할. 권한 수준은 ADMIN > OPERATOR > VIEWER 순이다.
 */
public enum MemberRole {
    ADMIN,
    OPERATOR,
    VIEWER;

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
