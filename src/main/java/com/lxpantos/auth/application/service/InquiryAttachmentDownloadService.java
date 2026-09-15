package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.InquiryAttachmentNotFoundException;
import com.lxpantos.auth.application.port.in.DownloadInquiryAttachmentUseCase;
import com.lxpantos.auth.application.port.in.InquiryAttachmentDownload;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentStorage;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;

public class InquiryAttachmentDownloadService implements DownloadInquiryAttachmentUseCase {
    private final InquiryAttachmentQueryRepository attachmentQueryRepository;
    private final InquiryAttachmentStorage storage;

    public InquiryAttachmentDownloadService(InquiryAttachmentQueryRepository attachmentQueryRepository,
                                            InquiryAttachmentStorage storage) {
        this.attachmentQueryRepository = attachmentQueryRepository;
        this.storage = storage;
    }

    @Override
    public InquiryAttachmentDownload prepareDownload(Long inquiryId, Long attachmentId) {
        InquiryAttachment attachment = attachmentQueryRepository.findByInquiryIdAndId(inquiryId, attachmentId)
                .filter(value -> storage.exists(value.storageKey()))
                .orElseThrow(InquiryAttachmentNotFoundException::new);
        return new InquiryAttachmentDownload(attachment.storageKey(), attachment.originalFilename(),
                attachment.mediaType(), attachment.fileSize());
    }
}
