package com.lxpantos.auth.application.port.in;

public interface DownloadInquiryAttachmentUseCase {
    InquiryAttachmentDownload prepareDownload(Long inquiryId, Long attachmentId);
}
