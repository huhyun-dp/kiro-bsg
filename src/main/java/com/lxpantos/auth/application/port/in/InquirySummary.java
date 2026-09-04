package com.lxpantos.auth.application.port.in;

import java.time.LocalDateTime;

public record InquirySummary(
        Long id,
        String title,
        Long viewCount,
        LocalDateTime createdAt
) {
}
