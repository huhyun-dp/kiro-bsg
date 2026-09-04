package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

import java.util.List;
import java.util.Optional;

/**
 * 권한 관리 목록/감사 로그 읽기 포트 (CQRS 읽기).
 */
public interface AccessMemberQueryRepository {

    long count(String keyword, MemberRole role, MemberStatus status);

    Optional<CurrentMemberAccessView> findAccessById(Long memberId);

    List<AccessMemberView> search(String keyword, MemberRole role, MemberStatus status, int limit, int offset);

    long countAuditLogs(Long targetMemberId);

    List<AuditLogView> searchAuditLogs(Long targetMemberId, int limit, int offset);
}
