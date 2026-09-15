package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.InquiryAccessDeniedException;
import com.lxpantos.auth.application.exception.InquiryAttachmentStorageException;
import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.exception.InvalidInquiryAttachmentException;
import com.lxpantos.auth.application.port.in.CreateInquiryCommand;
import com.lxpantos.auth.application.port.in.CreateInquiryUseCase;
import com.lxpantos.auth.application.port.in.DeleteInquiryCommand;
import com.lxpantos.auth.application.port.in.DeleteInquiryUseCase;
import com.lxpantos.auth.application.port.in.PendingInquiryAttachment;
import com.lxpantos.auth.application.port.in.UpdateInquiryCommand;
import com.lxpantos.auth.application.port.in.UpdateInquiryResult;
import com.lxpantos.auth.application.port.in.UpdateInquiryUseCase;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentStorage;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.application.port.out.TransactionRunner;
import com.lxpantos.auth.domain.inquiry.Inquiry;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class InquiryService implements CreateInquiryUseCase, UpdateInquiryUseCase, DeleteInquiryUseCase {
    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);
    private static final int MAX_ATTACHMENT_COUNT = 5;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_TOTAL_ATTACHMENT_SIZE = 20L * 1024 * 1024;

    private final InquiryRepository inquiryRepository;
    private final InquiryAttachmentRepository attachmentRepository;
    private final InquiryAttachmentQueryRepository attachmentQueryRepository;
    private final InquiryAttachmentStorage attachmentStorage;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public InquiryService(InquiryRepository inquiryRepository, Clock clock) {
        this(inquiryRepository, new NoopAttachmentRepository(), new NoopAttachmentQueryRepository(),
                new NoopAttachmentStorage(), new DirectTransactionRunner(), clock);
    }

    /**
     * Retained for source compatibility with the existing creation-only composition.
     * Edit composition must use the constructor that receives the attachment query port.
     */
    public InquiryService(InquiryRepository inquiryRepository, InquiryAttachmentRepository attachmentRepository,
                          InquiryAttachmentStorage attachmentStorage, TransactionRunner transactionRunner, Clock clock) {
        this(inquiryRepository, attachmentRepository, new NoopAttachmentQueryRepository(), attachmentStorage, transactionRunner, clock);
    }

    public InquiryService(InquiryRepository inquiryRepository, InquiryAttachmentRepository attachmentRepository,
                          InquiryAttachmentQueryRepository attachmentQueryRepository, InquiryAttachmentStorage attachmentStorage,
                          TransactionRunner transactionRunner, Clock clock) {
        this.inquiryRepository = Objects.requireNonNull(inquiryRepository, "inquiryRepository must not be null");
        this.attachmentRepository = Objects.requireNonNull(attachmentRepository, "attachmentRepository must not be null");
        this.attachmentQueryRepository = Objects.requireNonNull(attachmentQueryRepository, "attachmentQueryRepository must not be null");
        this.attachmentStorage = Objects.requireNonNull(attachmentStorage, "attachmentStorage must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Long create(CreateInquiryCommand command) {
        List<PendingInquiryAttachment> attachments = command.attachments() == null ? List.of() : command.attachments();
        try {
            for (PendingInquiryAttachment attachment : attachments) {
                attachmentStorage.store(attachment.storageKey(), attachment.content());
            }
            LocalDateTime createdAt = LocalDateTime.now(clock);
            return transactionRunner.execute(() -> {
                Long inquiryId = inquiryRepository.save(new Inquiry(null, command.memberId(), command.title(), command.content(), 0L, createdAt));
                for (PendingInquiryAttachment attachment : attachments) {
                    attachmentRepository.save(new InquiryAttachment(null, inquiryId, attachment.storageKey(), attachment.originalFilename(),
                            attachment.mediaType(), attachment.fileSize(), createdAt));
                }
                return inquiryId;
            });
        } catch (IOException | RuntimeException e) {
            compensate(attachments);
            if (e instanceof RuntimeException runtimeException) throw runtimeException;
            throw new InquiryAttachmentStorageException(e);
        }
    }

    @Override
    public UpdateInquiryResult update(UpdateInquiryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        validateNewAttachments(command.newAttachments());

        List<PendingInquiryAttachment> storedAttachments = new ArrayList<>();
        try {
            UpdateInquiryResult result = transactionRunner.execute(() -> updateWithinTransaction(command, storedAttachments));
            removeDeletedFilesAfterCommit(result);
            return result;
        } catch (RuntimeException e) {
            compensate(storedAttachments);
            throw e;
        }
    }

    private UpdateInquiryResult updateWithinTransaction(UpdateInquiryCommand command,
                                                         List<PendingInquiryAttachment> storedAttachments) {
        Long ownerId = inquiryRepository.findOwnerIdForUpdate(command.inquiryId())
                .orElseThrow(() -> new InquiryNotFoundException(command.inquiryId()));
        requireAuthor(ownerId, command.actorMemberId());

        List<InquiryAttachment> existingAttachments = attachmentQueryRepository.findByInquiryId(command.inquiryId());
        List<InquiryAttachment> attachmentsToDelete = findAttachmentsToDelete(
                existingAttachments, command.attachmentIdsToDelete());
        validateFinalAttachmentLimits(existingAttachments, attachmentsToDelete, command.newAttachments());

        for (PendingInquiryAttachment attachment : command.newAttachments()) {
            storeNewAttachment(attachment);
            storedAttachments.add(attachment);
        }

        if (inquiryRepository.updateContent(command.inquiryId(), command.title(), command.content()) != 1) {
            throw new InquiryNotFoundException(command.inquiryId());
        }

        LocalDateTime updatedAt = LocalDateTime.now(clock);
        for (PendingInquiryAttachment attachment : command.newAttachments()) {
            attachmentRepository.save(new InquiryAttachment(null, command.inquiryId(), attachment.storageKey(),
                    attachment.originalFilename(), attachment.mediaType(), attachment.fileSize(), updatedAt));
        }
        for (InquiryAttachment attachment : attachmentsToDelete) {
            if (attachmentRepository.deleteByInquiryIdAndId(command.inquiryId(), attachment.id()) != 1) {
                throw new InvalidInquiryAttachmentException("삭제할 첨부파일이 문의에 속하지 않습니다.");
            }
        }
        return new UpdateInquiryResult(command.inquiryId(), attachmentsToDelete);
    }

    @Override
    public void delete(DeleteInquiryCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Long ownerId = requireOwnerId(command.inquiryId());
        requireAuthorOrAdmin(ownerId, command.actorMemberId(), command.actorAdmin());
        if (inquiryRepository.softDelete(command.inquiryId()) == 0) {
            throw new InquiryNotFoundException(command.inquiryId());
        }
    }

    private List<InquiryAttachment> findAttachmentsToDelete(List<InquiryAttachment> existingAttachments,
                                                              Set<Long> attachmentIdsToDelete) {
        Map<Long, InquiryAttachment> existingById = new HashMap<>();
        for (InquiryAttachment attachment : existingAttachments) {
            existingById.put(attachment.id(), attachment);
        }

        List<InquiryAttachment> attachmentsToDelete = new ArrayList<>();
        for (Long attachmentId : attachmentIdsToDelete) {
            InquiryAttachment attachment = existingById.get(attachmentId);
            if (attachment == null) {
                throw new InvalidInquiryAttachmentException("삭제할 첨부파일이 문의에 속하지 않습니다.");
            }
            attachmentsToDelete.add(attachment);
        }
        return List.copyOf(attachmentsToDelete);
    }

    private void validateNewAttachments(List<PendingInquiryAttachment> newAttachments) {
        Set<String> storageKeys = new HashSet<>();
        for (PendingInquiryAttachment attachment : newAttachments) {
            if (attachment == null || attachment.storageKey() == null || attachment.storageKey().isBlank()
                    || attachment.originalFilename() == null || attachment.originalFilename().isBlank()
                    || attachment.mediaType() == null || attachment.content() == null
                    || attachment.fileSize() <= 0 || attachment.fileSize() > MAX_FILE_SIZE
                    || !storageKeys.add(attachment.storageKey())) {
                throw new InvalidInquiryAttachmentException("첨부파일 정보가 올바르지 않습니다.");
            }
        }
    }

    private void validateFinalAttachmentLimits(List<InquiryAttachment> existingAttachments,
                                                List<InquiryAttachment> attachmentsToDelete,
                                                List<PendingInquiryAttachment> newAttachments) {
        Set<Long> deletedIds = attachmentsToDelete.stream().map(InquiryAttachment::id).collect(java.util.stream.Collectors.toSet());
        long retainedCount = 0;
        long retainedSize = 0;
        try {
            for (InquiryAttachment attachment : existingAttachments) {
                if (!deletedIds.contains(attachment.id())) {
                    if (attachment.fileSize() <= 0) {
                        throw new InvalidInquiryAttachmentException("첨부파일 메타데이터가 올바르지 않습니다.");
                    }
                    retainedCount = Math.addExact(retainedCount, 1);
                    retainedSize = Math.addExact(retainedSize, attachment.fileSize());
                }
            }
            long newSize = 0;
            for (PendingInquiryAttachment attachment : newAttachments) {
                newSize = Math.addExact(newSize, attachment.fileSize());
            }
            if (Math.addExact(retainedCount, newAttachments.size()) > MAX_ATTACHMENT_COUNT
                    || Math.addExact(retainedSize, newSize) > MAX_TOTAL_ATTACHMENT_SIZE) {
                throw new InvalidInquiryAttachmentException("첨부파일은 최대 5개, 전체 20 MiB까지 등록할 수 있습니다.");
            }
        } catch (ArithmeticException e) {
            throw new InvalidInquiryAttachmentException("첨부파일은 최대 5개, 전체 20 MiB까지 등록할 수 있습니다.");
        }
    }

    private void storeNewAttachment(PendingInquiryAttachment attachment) {
        try {
            attachmentStorage.store(attachment.storageKey(), attachment.content());
        } catch (IOException e) {
            throw new InquiryAttachmentStorageException(e);
        }
    }

    private void removeDeletedFilesAfterCommit(UpdateInquiryResult result) {
        for (InquiryAttachment attachment : result.deletedAttachments()) {
            try {
                attachmentStorage.delete(attachment.storageKey());
            } catch (IOException | RuntimeException cleanupFailure) {
                log.warn("Post-commit inquiry attachment cleanup failed; orphan reconciliation is required for inquiryId={}, attachmentId={}, failureType={}",
                        result.inquiryId(), attachment.id(), cleanupFailure.getClass().getSimpleName());
            }
        }
    }

    private void compensate(List<PendingInquiryAttachment> attachments) {
        for (PendingInquiryAttachment attachment : attachments) {
            try {
                attachmentStorage.delete(attachment.storageKey());
            } catch (IOException | RuntimeException cleanupFailure) {
                log.warn("Unable to compensate newly stored inquiry attachment; orphan reconciliation is required, failureType={}",
                        cleanupFailure.getClass().getSimpleName());
            }
        }
    }

    private Long requireOwnerId(Long inquiryId) {
        return inquiryRepository.findOwnerId(inquiryId).orElseThrow(() -> new InquiryNotFoundException(inquiryId));
    }

    private void requireAuthor(Long ownerId, Long actorMemberId) {
        if (!Objects.equals(ownerId, actorMemberId)) {
            throw new InquiryAccessDeniedException();
        }
    }

    private void requireAuthorOrAdmin(Long ownerId, Long actorMemberId, boolean actorAdmin) {
        if (!actorAdmin && !Objects.equals(ownerId, actorMemberId)) throw new InquiryAccessDeniedException();
    }

    private static final class DirectTransactionRunner implements TransactionRunner {
        @Override public <T> T execute(java.util.function.Supplier<T> action) { return action.get(); }
    }

    private static final class NoopAttachmentRepository implements InquiryAttachmentRepository {
        @Override public void save(InquiryAttachment attachment) { }
        @Override public int deleteByInquiryIdAndId(Long inquiryId, Long attachmentId) { return 0; }
    }

    private static final class NoopAttachmentQueryRepository implements InquiryAttachmentQueryRepository {
        @Override public List<InquiryAttachment> findByInquiryId(Long inquiryId) { return List.of(); }
        @Override public java.util.Optional<InquiryAttachment> findByInquiryIdAndId(Long inquiryId, Long attachmentId) {
            return java.util.Optional.empty();
        }
        @Override public Set<String> findAllStorageKeys() { return Set.of(); }
    }

    private static final class NoopAttachmentStorage implements InquiryAttachmentStorage {
        @Override public void store(String key, com.lxpantos.auth.application.port.in.AttachmentContent content) { }
        @Override public java.io.InputStream open(String key) throws IOException { throw new IOException("unavailable"); }
        @Override public boolean exists(String key) { return false; }
        @Override public void delete(String key) { }
        @Override public List<String> findKeysOlderThan(java.time.Duration age) { return List.of(); }
    }
}
