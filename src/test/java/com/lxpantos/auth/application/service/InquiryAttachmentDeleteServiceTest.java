package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.InquiryAccessDeniedException;
import com.lxpantos.auth.application.exception.InquiryAttachmentNotFoundException;
import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.AttachmentContent;
import com.lxpantos.auth.application.port.in.DeleteInquiryAttachmentCommand;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentStorage;
import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.application.port.out.TransactionRunner;
import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;
import com.lxpantos.auth.domain.inquiry.Inquiry;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the immediate per-attachment delete use case (task 9.10).
 */
class InquiryAttachmentDeleteServiceTest {

    private static final Long INQUIRY_ID = 1L;
    private static final Long OWNER_ID = 42L;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2024-06-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private FakeInquiryRepository inquiryRepository;
    private FakeAttachmentRepository attachmentRepository;
    private FakeAttachmentQueryRepository attachmentQueryRepository;
    private FakeAttachmentStorage attachmentStorage;
    private InquiryService service;

    @BeforeEach
    void setUp() {
        inquiryRepository = new FakeInquiryRepository();
        attachmentQueryRepository = new FakeAttachmentQueryRepository();
        attachmentRepository = new FakeAttachmentRepository(attachmentQueryRepository);
        attachmentStorage = new FakeAttachmentStorage();
        inquiryRepository.ownerId = OWNER_ID;
        service = new InquiryService(inquiryRepository, attachmentRepository, attachmentQueryRepository,
                attachmentStorage, new DirectTransactionRunner(), fixedClock);
    }

    @Test
    void authorCanDeleteAttachmentAndFileIsRemovedAfterCommit() {
        InquiryAttachment first = attachmentQueryRepository.add(INQUIRY_ID, "first.pdf", "key-1", 10L);
        attachmentQueryRepository.add(INQUIRY_ID, "second.png", "key-2", 20L);

        service.deleteAttachment(new DeleteInquiryAttachmentCommand(INQUIRY_ID, first.id(), OWNER_ID));

        assertThat(attachmentRepository.deletedKeys).containsExactly(INQUIRY_ID + ":" + first.id());
        assertThat(attachmentStorage.deletedStorageKeys).containsExactly("key-1");
    }

    @Test
    void authorCanDeleteLastAttachment() {
        InquiryAttachment only = attachmentQueryRepository.add(INQUIRY_ID, "only.pdf", "key-only", 5L);

        service.deleteAttachment(new DeleteInquiryAttachmentCommand(INQUIRY_ID, only.id(), OWNER_ID));

        assertThat(attachmentRepository.deletedKeys).containsExactly(INQUIRY_ID + ":" + only.id());
        assertThat(attachmentStorage.deletedStorageKeys).containsExactly("key-only");
        assertThat(attachmentQueryRepository.findByInquiryId(INQUIRY_ID)).isEmpty();
    }

    @Test
    void nonAuthorIsDeniedAndNothingIsRemoved() {
        InquiryAttachment first = attachmentQueryRepository.add(INQUIRY_ID, "first.pdf", "key-1", 10L);

        assertThatThrownBy(() -> service.deleteAttachment(
                new DeleteInquiryAttachmentCommand(INQUIRY_ID, first.id(), 7L)))
                .isInstanceOf(InquiryAccessDeniedException.class);

        assertThat(attachmentRepository.deletedKeys).isEmpty();
        assertThat(attachmentStorage.deletedStorageKeys).isEmpty();
    }

    @Test
    void missingInquiryThrowsNotFound() {
        inquiryRepository.ownerId = null;

        assertThatThrownBy(() -> service.deleteAttachment(
                new DeleteInquiryAttachmentCommand(INQUIRY_ID, 99L, OWNER_ID)))
                .isInstanceOf(InquiryNotFoundException.class);
        assertThat(attachmentStorage.deletedStorageKeys).isEmpty();
    }

    @Test
    void attachmentBelongingToAnotherInquiryReturnsNotFoundAndRemovesNothing() {
        // attachment exists but belongs to a different inquiry (id=2), not the requested INQUIRY_ID
        InquiryAttachment foreign = attachmentQueryRepository.add(2L, "other.pdf", "key-other", 10L);

        assertThatThrownBy(() -> service.deleteAttachment(
                new DeleteInquiryAttachmentCommand(INQUIRY_ID, foreign.id(), OWNER_ID)))
                .isInstanceOf(InquiryAttachmentNotFoundException.class);

        assertThat(attachmentRepository.deletedKeys).isEmpty();
        assertThat(attachmentStorage.deletedStorageKeys).isEmpty();
        assertThat(attachmentQueryRepository.findByInquiryId(2L)).hasSize(1);
    }

    @Test
    void postCommitFileRemovalFailureIsLoggedAndRequestStillSucceeds() {
        InquiryAttachment first = attachmentQueryRepository.add(INQUIRY_ID, "first.pdf", "key-1", 10L);
        attachmentStorage.failDelete = true;

        // Should not throw even though storage.delete fails after commit.
        service.deleteAttachment(new DeleteInquiryAttachmentCommand(INQUIRY_ID, first.id(), OWNER_ID));

        assertThat(attachmentRepository.deletedKeys).containsExactly(INQUIRY_ID + ":" + first.id());
        assertThat(attachmentStorage.deleteAttempts).containsExactly("key-1");
    }

    // ── Fakes ─────────────────────────────────────────────

    private static final class DirectTransactionRunner implements TransactionRunner {
        @Override public <T> T execute(Supplier<T> action) { return action.get(); }
    }

    private static final class FakeInquiryRepository implements InquiryRepository {
        Long ownerId;
        @Override public Long save(Inquiry inquiry) { return 1L; }
        @Override public void incrementViewCount(Long id) { }
        @Override public Optional<InquiryQueryResult> findById(Long id) { return Optional.empty(); }
        @Override public Optional<Long> findOwnerId(Long id) { return Optional.ofNullable(ownerId); }
        @Override public int updateContent(Long id, String title, String content) { return 1; }
        @Override public int softDelete(Long id) { return 1; }
    }

    private static final class FakeAttachmentRepository implements InquiryAttachmentRepository {
        final FakeAttachmentQueryRepository query;
        final List<String> deletedKeys = new ArrayList<>();

        FakeAttachmentRepository(FakeAttachmentQueryRepository query) { this.query = query; }

        @Override public void save(InquiryAttachment attachment) { }

        @Override
        public int deleteByInquiryIdAndId(Long inquiryId, Long attachmentId) {
            boolean removed = query.removeIfBelongs(inquiryId, attachmentId);
            if (removed) {
                deletedKeys.add(inquiryId + ":" + attachmentId);
                return 1;
            }
            return 0;
        }
    }

    private static final class FakeAttachmentQueryRepository implements InquiryAttachmentQueryRepository {
        private final Map<Long, InquiryAttachment> byId = new LinkedHashMap<>();
        private long sequence = 1;

        InquiryAttachment add(Long inquiryId, String filename, String storageKey, long size) {
            long id = sequence++;
            InquiryAttachment attachment = new InquiryAttachment(id, inquiryId, storageKey, filename,
                    filename.endsWith(".pdf") ? AttachmentMediaType.PDF : AttachmentMediaType.PNG,
                    size, LocalDateTime.now());
            byId.put(id, attachment);
            return attachment;
        }

        boolean removeIfBelongs(Long inquiryId, Long attachmentId) {
            InquiryAttachment attachment = byId.get(attachmentId);
            if (attachment != null && attachment.inquiryId().equals(inquiryId)) {
                byId.remove(attachmentId);
                return true;
            }
            return false;
        }

        @Override
        public List<InquiryAttachment> findByInquiryId(Long inquiryId) {
            return byId.values().stream().filter(a -> a.inquiryId().equals(inquiryId)).toList();
        }

        @Override
        public Optional<InquiryAttachment> findByInquiryIdAndId(Long inquiryId, Long attachmentId) {
            return Optional.ofNullable(byId.get(attachmentId)).filter(a -> a.inquiryId().equals(inquiryId));
        }

        @Override public Set<String> findAllStorageKeys() {
            return byId.values().stream().map(InquiryAttachment::storageKey).collect(java.util.stream.Collectors.toSet());
        }
    }

    private static final class FakeAttachmentStorage implements InquiryAttachmentStorage {
        final List<String> deleteAttempts = new ArrayList<>();
        final List<String> deletedStorageKeys = new ArrayList<>();
        boolean failDelete;

        @Override public void store(String storageKey, AttachmentContent content) { }
        @Override public InputStream open(String storageKey) throws IOException { throw new IOException("unavailable"); }
        @Override public boolean exists(String storageKey) { return true; }
        @Override public void delete(String storageKey) throws IOException {
            deleteAttempts.add(storageKey);
            if (failDelete) throw new IOException("cannot delete");
            deletedStorageKeys.add(storageKey);
        }
        @Override public List<String> findKeysOlderThan(Duration age) { return List.of(); }
    }
}
