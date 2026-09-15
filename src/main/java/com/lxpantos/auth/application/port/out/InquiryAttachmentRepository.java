package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.inquiry.InquiryAttachment;

public interface InquiryAttachmentRepository {
    void save(InquiryAttachment attachment);

    /**
     * Deletes metadata only when the attachment is associated with the supplied inquiry.
     * A result of zero means the attachment was not verified as belonging to that inquiry.
     */
    default int deleteByInquiryIdAndId(Long inquiryId, Long attachmentId) {
        throw new UnsupportedOperationException("Verified inquiry attachment deletion is not implemented");
    }
}
