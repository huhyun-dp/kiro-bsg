package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.InquiryAttachmentRepository;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisInquiryAttachmentRepository implements InquiryAttachmentRepository {
    private final InquiryAttachmentMapper mapper;
    public MyBatisInquiryAttachmentRepository(InquiryAttachmentMapper mapper) { this.mapper = mapper; }
    @Override public void save(InquiryAttachment attachment) {
        mapper.insert(new InquiryAttachmentPersistenceModel(attachment.id(), attachment.inquiryId(), attachment.storageKey(),
                attachment.originalFilename(), attachment.mediaType().contentType(), attachment.fileSize(), attachment.createdAt()));
    }

    @Override public int deleteByInquiryIdAndId(Long inquiryId, Long attachmentId) {
        return mapper.deleteByInquiryIdAndId(inquiryId, attachmentId);
    }
}
