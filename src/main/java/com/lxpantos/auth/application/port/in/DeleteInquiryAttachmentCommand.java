package com.lxpantos.auth.application.port.in;

import java.util.Objects;

/**
 * Immediate per-attachment deletion request. The edit screen deletes a single existing
 * attachment right away instead of the previous deferred {@code deleteAttachmentIds} flow.
 *
 * <p>Only application-level identifiers cross this boundary; Spring types, {@code MultipartFile},
 * and file {@code Path} values are kept out of the application/domain boundary. Deletion is
 * author-only and always verifies {@code (inquiryId, attachmentId)} ownership.
 */
public record DeleteInquiryAttachmentCommand(
        Long inquiryId,
        Long attachmentId,
        Long actorMemberId
) {
    public DeleteInquiryAttachmentCommand {
        Objects.requireNonNull(inquiryId, "inquiryId must not be null");
        Objects.requireNonNull(attachmentId, "attachmentId must not be null");
        Objects.requireNonNull(actorMemberId, "actorMemberId must not be null");
    }
}
