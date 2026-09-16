package com.lxpantos.auth.application.port.in;

import java.time.LocalDateTime;

public record InquirySummary(
        Long id,
        String title,
        Long viewCount,
        LocalDateTime createdAt,
        long attachmentCount
) {
    public boolean hasAttachments() {
        return attachmentCount > 0;
    }
}
