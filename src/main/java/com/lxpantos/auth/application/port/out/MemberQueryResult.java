package com.lxpantos.auth.application.port.out;

import java.time.LocalDateTime;

public record MemberQueryResult(
        Long id,
        String name,
        String email,
        String phoneNumber,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
