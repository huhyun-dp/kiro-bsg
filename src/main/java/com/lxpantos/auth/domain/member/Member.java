package com.lxpantos.auth.domain.member;

import java.time.LocalDateTime;

public record Member(
        Long id,
        String email,
        String passwordHash,
        String name,
        LocalDateTime createdAt
) {
    public static Member newMember(String email, String passwordHash, String name, LocalDateTime createdAt) {
        return new Member(null, email, passwordHash, name, createdAt);
    }
}

