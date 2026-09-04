package com.lxpantos.auth.application.port.in;

import java.time.LocalDateTime;

public record InquiryDetail(
        Long id,
        String authorName,
        String title,
        String content,
        Long viewCount,
        LocalDateTime createdAt
) {
}
