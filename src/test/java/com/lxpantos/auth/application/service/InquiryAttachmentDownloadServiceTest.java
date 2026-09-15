package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.InquiryAttachmentNotFoundException;
import com.lxpantos.auth.application.port.in.AttachmentContent;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentStorage;
import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryAttachmentDownloadServiceTest {
    @Test void onlyReturnsExistingAttachmentBelongingToRequestedInquiry() {
        InquiryAttachment attachment = new InquiryAttachment(7L, 3L, "00000000-0000-0000-0000-000000000001", "proof.pdf", AttachmentMediaType.PDF, 5, LocalDateTime.now());
        InquiryAttachmentDownloadService service = new InquiryAttachmentDownloadService(new FakeQuery(attachment), new ExistingStorage());
        assertThat(service.prepareDownload(3L, 7L).originalFilename()).isEqualTo("proof.pdf");
        assertThatThrownBy(() -> service.prepareDownload(4L, 7L)).isInstanceOf(InquiryAttachmentNotFoundException.class);
    }
    private record FakeQuery(InquiryAttachment attachment) implements InquiryAttachmentQueryRepository {
        @Override public List<InquiryAttachment> findByInquiryId(Long id) { return List.of(); }
        @Override public Optional<InquiryAttachment> findByInquiryIdAndId(Long inquiryId, Long attachmentId) { return attachment.inquiryId().equals(inquiryId) && attachment.id().equals(attachmentId) ? Optional.of(attachment) : Optional.empty(); }
        @Override public Set<String> findAllStorageKeys() { return Set.of(); }
    }
    private static class ExistingStorage implements InquiryAttachmentStorage {
        @Override public void store(String key, AttachmentContent content) { }
        @Override public InputStream open(String key) { return InputStream.nullInputStream(); }
        @Override public boolean exists(String key) { return true; }
        @Override public void delete(String key) { }
        @Override public List<String> findKeysOlderThan(java.time.Duration age) { return List.of(); }
    }
}
