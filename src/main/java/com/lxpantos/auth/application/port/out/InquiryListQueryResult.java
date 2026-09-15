package com.lxpantos.auth.application.port.out;

import java.time.LocalDateTime;

/**
 * Metadata-free list projection for inquiries. Attachment details are intentionally excluded.
 */
public record InquiryListQueryResult(
        Long id,
        String title,
        Long viewCount,
        LocalDateTime createdAt,
        boolean hasAttachments
) {
}
