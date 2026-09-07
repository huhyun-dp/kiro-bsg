package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

import java.time.LocalDateTime;

/**
 * 권한 변경 감사 로그 한 건. 요청 IP 는 조회 화면에 노출되므로 마스킹된 값을 담는다.
 */
public record AccessAuditLogEntry(
        Long id,
        Long targetMemberId,
        Long actorMemberId,
        String actorName,
        MemberRole beforeRole,
        MemberStatus beforeStatus,
        MemberRole afterRole,
        MemberStatus afterStatus,
        String reason,
        String maskedRequestIp,
        LocalDateTime createdAt
) {
}
