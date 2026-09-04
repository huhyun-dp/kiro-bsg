package com.lxpantos.auth.application.port.out;

import java.time.LocalDateTime;

public record InquiryQueryResult(
        Long id,
        String authorName,
        String title,
        String content,
        Long viewCount,
        LocalDateTime createdAt
) {
}
