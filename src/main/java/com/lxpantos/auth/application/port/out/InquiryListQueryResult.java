package com.lxpantos.auth.application.port.out;

import java.time.LocalDateTime;

/**
 * Metadata-free list projection for inquiries. Attachment details (id/storage key/filename/path/
 * download URL) are intentionally excluded; only the aggregate attachment count is exposed so the
 * list can render "첨부파일(N)". {@code hasAttachments} is derived from the count.
 */
public record InquiryListQueryResult(
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
