package com.lxpantos.auth.domain.inquiry;

import java.time.LocalDateTime;

public record InquiryAttachment(
        Long id,
        Long inquiryId,
        String storageKey,
        String originalFilename,
        AttachmentMediaType mediaType,
        long fileSize,
        LocalDateTime createdAt
) {
}
