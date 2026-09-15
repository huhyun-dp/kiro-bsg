package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class MyBatisInquiryAttachmentQueryRepository implements InquiryAttachmentQueryRepository {
    private final InquiryAttachmentMapper mapper;
    public MyBatisInquiryAttachmentQueryRepository(InquiryAttachmentMapper mapper) { this.mapper = mapper; }
    @Override public List<InquiryAttachment> findByInquiryId(Long inquiryId) {
        return mapper.findByInquiryId(inquiryId).stream().map(this::toDomain).toList();
    }
    @Override public Optional<InquiryAttachment> findByInquiryIdAndId(Long inquiryId, Long attachmentId) {
        return mapper.findByInquiryIdAndId(inquiryId, attachmentId).map(this::toDomain);
    }
    @Override public Set<String> findAllStorageKeys() { return Set.copyOf(mapper.findAllStorageKeys()); }
    private InquiryAttachment toDomain(InquiryAttachmentPersistenceModel value) {
        return new InquiryAttachment(value.getId(), value.getInquiryId(), value.getStorageKey(), value.getOriginalFilename(),
                AttachmentMediaType.valueOf(value.getContentType().equals("application/pdf") ? "PDF" : value.getContentType().equals("image/png") ? "PNG" : "JPEG"),
                value.getFileSize(), value.getCreatedAt());
    }
}
