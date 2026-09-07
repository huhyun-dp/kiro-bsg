package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

/**
 * 세션 검증 시 조회하는 회원의 현재 역할/상태.
 */
public record CurrentMemberAccess(Long id, MemberRole role, MemberStatus status) {
}
