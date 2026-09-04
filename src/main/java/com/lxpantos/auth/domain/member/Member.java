package com.lxpantos.auth.domain.member;

import java.time.LocalDateTime;

public record Member(
        Long id,
        String email,
        String passwordHash,
        String name,
        String phoneNumber,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        MemberRole role,
        MemberStatus status,
        long version,
        LocalDateTime roleUpdatedAt
) {
    public static Member newMember(
            String email,
            String passwordHash,
            String name,
            String phoneNumber,
            LocalDateTime createdAt
    ) {
        return new Member(
                null,
                email,
                passwordHash,
                name,
                phoneNumber,
                createdAt,
                null,
                MemberRole.VIEWER,
                MemberStatus.ACTIVE,
                0L,
                null
        );
    }
}
