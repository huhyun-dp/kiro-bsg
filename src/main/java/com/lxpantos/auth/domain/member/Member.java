package com.lxpantos.auth.domain.member;

import java.time.LocalDateTime;

public record Member(
        Long id,
        String email,
        String passwordHash,
        String name,
        String phoneNumber,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
    public static Member newMember(
            String email,
            String passwordHash,
            String name,
            String phoneNumber,
            LocalDateTime createdAt
    ) {
        return new Member(null, email, passwordHash, name, phoneNumber, createdAt, null);
    }
}
