package com.lxpantos.auth.application.port.in;

import java.time.LocalDateTime;

public record MemberSummary(Long id, String name, String email, LocalDateTime createdAt) {
}
