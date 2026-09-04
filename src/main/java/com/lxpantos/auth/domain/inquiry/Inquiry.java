package com.lxpantos.auth.domain.inquiry;

import java.time.LocalDateTime;

public record Inquiry(
        Long id,
        Long memberId,
        String title,
        String content,
        Long viewCount,
        LocalDateTime createdAt
) {
}
