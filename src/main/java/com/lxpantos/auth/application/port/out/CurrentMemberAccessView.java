package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

/**
 * 세션 검증용 회원 역할/상태 읽기 모델.
 */
public record CurrentMemberAccessView(Long id, MemberRole role, MemberStatus status) {
}
