package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.inquiry.InquiryAttachment;

import java.util.List;
import java.util.Objects;

/**
 * Metadata produced by an inquiry edit. Deleted attachments are retained as metadata only
 * so the application service can remove their private stored files after DB commit.
 */
public record UpdateInquiryResult(
        Long inquiryId,
        List<InquiryAttachment> deletedAttachments
) {
    public UpdateInquiryResult {
        Objects.requireNonNull(inquiryId, "inquiryId must not be null");
        deletedAttachments = List.copyOf(Objects.requireNonNull(deletedAttachments, "deletedAttachments must not be null"));
    }

    public static UpdateInquiryResult withoutDeletedAttachments(Long inquiryId) {
        return new UpdateInquiryResult(inquiryId, List.of());
    }
}
