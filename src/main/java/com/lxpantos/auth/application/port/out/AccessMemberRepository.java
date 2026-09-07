package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.member.Member;
import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 권한 관리 쓰기 포트 (CQRS 쓰기). 역할/상태 변경과 감사 로그 저장을 담당한다.
 */
public interface AccessMemberRepository {

    Optional<Member> findById(Long memberId);

    Optional<Member> findByEmail(String email);

    /**
     * 낙관적 잠금으로 회원의 역할/상태를 갱신한다. version 이 일치할 때만 갱신되며,
     * 갱신된 행 수를 반환한다(0 이면 충돌 또는 미존재).
     */
    int updateAccess(
            Long memberId,
            MemberRole role,
            MemberStatus status,
            LocalDateTime roleUpdatedAt,
            long expectedVersion
    );

    /**
     * 활성 관리자를 강등/정지할 때 사용하는 가드 업데이트.
     * version 이 일치하고, 대상 회원을 제외한 다른 활성 관리자가 1명 이상 있을 때만 갱신한다.
     * (동시 강등으로 활성 관리자가 0명이 되는 경쟁 상태를 단일 SQL 로 차단)
     */
    int updateAccessKeepingLastAdmin(
            Long memberId,
            MemberRole role,
            MemberStatus status,
            LocalDateTime roleUpdatedAt,
            long expectedVersion
    );

    void saveAuditLog(AuditLogEntry entry);
}
