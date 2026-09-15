package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;

public record InquiryAttachmentSummary(
        Long id,
        String originalFilename,
        AttachmentMediaType mediaType,
        long fileSize
) {
}
