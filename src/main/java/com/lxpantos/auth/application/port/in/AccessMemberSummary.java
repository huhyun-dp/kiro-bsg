package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

import java.time.LocalDateTime;

/**
 * 권한 관리 목록에 표시되는 회원 한 명의 요약 정보. 휴대폰 번호는 마스킹된 값이다.
 */
public record AccessMemberSummary(
        Long id,
        String name,
        String email,
        String maskedPhoneNumber,
        MemberRole role,
        MemberStatus status,
        long version,
        LocalDateTime roleUpdatedAt,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
