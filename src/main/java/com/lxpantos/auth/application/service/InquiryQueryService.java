package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.InquiryAttachmentSummary;
import com.lxpantos.auth.application.port.in.InquiryDetail;
import com.lxpantos.auth.application.port.in.InquiryPage;
import com.lxpantos.auth.application.port.in.InquiryQueryUseCase;
import com.lxpantos.auth.application.port.in.InquirySummary;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import com.lxpantos.auth.application.port.out.InquiryRepository;

import java.util.List;
import java.util.Optional;

public class InquiryQueryService implements InquiryQueryUseCase {
    private final InquiryQueryRepository inquiryQueryRepository;
    private final InquiryRepository inquiryRepository;
    private final InquiryAttachmentQueryRepository attachmentQueryRepository;

    public InquiryQueryService(InquiryQueryRepository inquiryQueryRepository, InquiryRepository inquiryRepository) {
        this(inquiryQueryRepository, inquiryRepository, new EmptyAttachmentQueryRepository());
    }
    public InquiryQueryService(InquiryQueryRepository inquiryQueryRepository, InquiryRepository inquiryRepository,
                               InquiryAttachmentQueryRepository attachmentQueryRepository) {
        this.inquiryQueryRepository = inquiryQueryRepository;
        this.inquiryRepository = inquiryRepository;
        this.attachmentQueryRepository = attachmentQueryRepository;
    }
    @Override public InquiryPage getPage(int page, int pageSize) {
        long totalCount = inquiryQueryRepository.countAll();
        int totalPages = totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        List<InquirySummary> items = inquiryQueryRepository.findPage((safePage - 1) * pageSize, pageSize).stream()
                .map(value -> new InquirySummary(
                        value.id(), value.title(), value.viewCount(), value.createdAt(), value.hasAttachments()))
                .toList();
        return new InquiryPage(items, safePage, totalPages, totalCount);
    }
    @Override public InquiryDetail getDetail(Long id) {
        InquiryQueryResult result = inquiryRepository.findById(id).orElseThrow(() -> new InquiryNotFoundException(id));
        inquiryRepository.incrementViewCount(id);
        return toDetail(result, result.viewCount() + 1L);
    }
    @Override public InquiryDetail getDetailWithoutView(Long id) {
        InquiryQueryResult result = inquiryRepository.findById(id).orElseThrow(() -> new InquiryNotFoundException(id));
        return toDetail(result, result.viewCount());
    }
    @Override public Optional<Long> findOwnerId(Long id) { return inquiryRepository.findOwnerId(id); }
    private InquiryDetail toDetail(InquiryQueryResult result, long viewCount) {
        List<InquiryAttachmentSummary> attachments = attachmentQueryRepository.findByInquiryId(result.id()).stream()
                .map(value -> new InquiryAttachmentSummary(value.id(), value.originalFilename(), value.mediaType(), value.fileSize())).toList();
        return new InquiryDetail(result.id(), result.authorName(), result.title(), result.content(), viewCount, result.createdAt(), attachments);
    }
    private static final class EmptyAttachmentQueryRepository implements InquiryAttachmentQueryRepository {
        @Override public List<com.lxpantos.auth.domain.inquiry.InquiryAttachment> findByInquiryId(Long id) { return List.of(); }
        @Override public Optional<com.lxpantos.auth.domain.inquiry.InquiryAttachment> findByInquiryIdAndId(Long inquiryId, Long attachmentId) { return Optional.empty(); }
        @Override public java.util.Set<String> findAllStorageKeys() { return java.util.Set.of(); }
    }
}
