package com.lxpantos.auth.application.port.in;

import java.time.LocalDateTime;
import java.util.List;

public record InquiryDetail(
        Long id,
        String authorName,
        String title,
        String content,
        Long viewCount,
        LocalDateTime createdAt,
        List<InquiryAttachmentSummary> attachments
) {
    public InquiryDetail(Long id, String authorName, String title, String content, Long viewCount, LocalDateTime createdAt) {
        this(id, authorName, title, content, viewCount, createdAt, List.of());
    }
}
