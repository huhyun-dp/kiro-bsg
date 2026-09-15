package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.inquiry.InquiryAttachment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InquiryAttachmentQueryRepository {
    List<InquiryAttachment> findByInquiryId(Long inquiryId);

    /**
     * Metadata-only retained attachment summary derived from the authoritative attachment list.
     */
    default InquiryAttachmentAggregate getAggregateByInquiryId(Long inquiryId) {
        List<InquiryAttachment> attachments = findByInquiryId(inquiryId);
        return new InquiryAttachmentAggregate(
                (long) attachments.size(),
                attachments.stream().mapToLong(InquiryAttachment::fileSize).sum()
        );
    }

    Optional<InquiryAttachment> findByInquiryIdAndId(Long inquiryId, Long attachmentId);
    Set<String> findAllStorageKeys();
}
