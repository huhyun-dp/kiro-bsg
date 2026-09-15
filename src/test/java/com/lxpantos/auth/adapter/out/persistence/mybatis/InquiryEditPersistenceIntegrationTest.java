package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.InquiryAttachmentAggregate;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentRepository;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InquiryEditPersistenceIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private InquiryAttachmentRepository attachmentRepository;
    @Autowired private InquiryAttachmentQueryRepository attachmentQueryRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private Long inquiryId;
    private Long otherInquiryId;
    private Long firstAttachmentId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM inquiry_attachments");
        jdbcTemplate.update("DELETE FROM inquiries");
        jdbcTemplate.update("DELETE FROM members WHERE email IN (?, ?)", "inquiry-edit-owner@example.com", "inquiry-edit-other@example.com");
        insertMember("inquiry-edit-owner@example.com", "작성자");
        insertMember("inquiry-edit-other@example.com", "다른작성자");
        Long ownerId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE email = ?", Long.class, "inquiry-edit-owner@example.com");
        Long otherOwnerId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE email = ?", Long.class, "inquiry-edit-other@example.com");
        inquiryId = insertInquiry(ownerId, "원래 제목", "원래 내용");
        otherInquiryId = insertInquiry(otherOwnerId, "다른 제목", "다른 내용");
        firstAttachmentId = insertAttachment(inquiryId, "first.pdf", 4L);
        insertAttachment(inquiryId, "second.png", 7L);
    }

    @Test
    void loadsOwnerUnderWriteLockAndUpdatesOnlyActiveInquiryBody() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            assertThat(inquiryRepository.findOwnerIdForUpdate(inquiryId)).isPresent();
            assertThat(inquiryRepository.updateContent(inquiryId, "수정 제목", "수정 내용")).isEqualTo(1);
        });

        assertThat(jdbcTemplate.queryForObject("SELECT title FROM inquiries WHERE id = ?", String.class, inquiryId))
                .isEqualTo("수정 제목");
        assertThat(jdbcTemplate.queryForObject("SELECT content FROM inquiries WHERE id = ?", String.class, inquiryId))
                .isEqualTo("수정 내용");
    }

    @Test
    void aggregatesAndListsOnlyAttachmentsBelongingToTheActiveInquiry() {
        InquiryAttachmentAggregate aggregate = attachmentQueryRepository.getAggregateByInquiryId(inquiryId);

        assertThat(aggregate.count()).isEqualTo(2);
        assertThat(aggregate.totalFileSize()).isEqualTo(11L);
        assertThat(attachmentQueryRepository.findByInquiryId(inquiryId))
                .extracting(InquiryAttachment::originalFilename)
                .containsExactly("first.pdf", "second.png");
    }

    @Test
    void deletesAttachmentOnlyWhenItBelongsToTheSpecifiedInquiry() {
        assertThat(attachmentRepository.deleteByInquiryIdAndId(otherInquiryId, firstAttachmentId)).isZero();
        assertThat(attachmentQueryRepository.findByInquiryId(inquiryId)).hasSize(2);

        assertThat(attachmentRepository.deleteByInquiryIdAndId(inquiryId, firstAttachmentId)).isEqualTo(1);
        assertThat(attachmentQueryRepository.findByInquiryId(inquiryId)).hasSize(1);
    }

    private void insertMember(String email, String name) {
        jdbcTemplate.update("INSERT INTO members (email, password_hash, name, created_at, role, status, version) VALUES (?, ?, ?, ?, 'VIEWER', 'ACTIVE', 0)",
                email, "$2a$04$abcdefghijklmnopqrstuv", name, LocalDateTime.now());
    }

    private Long insertInquiry(Long memberId, String title, String content) {
        jdbcTemplate.update("INSERT INTO inquiries (member_id, title, content, view_count, created_at, deleted) VALUES (?, ?, ?, 0, ?, FALSE)",
                memberId, title, content, LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM inquiries", Long.class);
    }

    private Long insertAttachment(Long targetInquiryId, String filename, long size) {
        jdbcTemplate.update("INSERT INTO inquiry_attachments (inquiry_id, storage_key, original_filename, content_type, file_size, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                targetInquiryId, UUID.randomUUID().toString(), filename,
                filename.endsWith(".pdf") ? AttachmentMediaType.PDF.contentType() : AttachmentMediaType.PNG.contentType(),
                size, LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM inquiry_attachments", Long.class);
    }
}
