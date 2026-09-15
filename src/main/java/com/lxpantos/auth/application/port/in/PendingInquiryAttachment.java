package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;

public record PendingInquiryAttachment(
        String storageKey,
        String originalFilename,
        AttachmentMediaType mediaType,
        long fileSize,
        AttachmentContent content
) {
}
