package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

import java.time.LocalDateTime;

/**
 * 권한 관리 목록 조회용 읽기 모델. 마스킹 전 원본 휴대폰 번호를 담는다.
 */
public record AccessMemberView(
        Long id,
        String name,
        String email,
        String phoneNumber,
        MemberRole role,
        MemberStatus status,
        long version,
        LocalDateTime roleUpdatedAt,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
