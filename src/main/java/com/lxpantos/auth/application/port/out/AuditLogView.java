package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

import java.time.LocalDateTime;

/**
 * 감사 로그 조회용 읽기 모델. 요청 IP 는 마스킹 전 원본을 담는다.
 */
public record AuditLogView(
        Long id,
        Long targetMemberId,
        Long actorMemberId,
        String actorName,
        MemberRole beforeRole,
        MemberStatus beforeStatus,
        MemberRole afterRole,
        MemberStatus afterStatus,
        String reason,
        String requestIp,
        LocalDateTime createdAt
) {
}
