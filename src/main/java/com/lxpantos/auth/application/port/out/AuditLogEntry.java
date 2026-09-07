package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

import java.time.LocalDateTime;

/**
 * 감사 로그 저장용 쓰기 모델.
 */
public record AuditLogEntry(
        Long targetMemberId,
        Long actorMemberId,
        MemberRole beforeRole,
        MemberStatus beforeStatus,
        MemberRole afterRole,
        MemberStatus afterStatus,
        String reason,
        String requestIp,
        LocalDateTime createdAt
) {
}
