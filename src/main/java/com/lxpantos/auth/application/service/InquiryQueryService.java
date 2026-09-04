package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.InquiryDetail;
import com.lxpantos.auth.application.port.in.InquiryPage;
import com.lxpantos.auth.application.port.in.InquiryQueryUseCase;
import com.lxpantos.auth.application.port.in.InquirySummary;
import com.lxpantos.auth.application.port.out.InquiryQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import com.lxpantos.auth.application.port.out.InquiryRepository;

import java.util.List;

public class InquiryQueryService implements InquiryQueryUseCase {

    private final InquiryQueryRepository inquiryQueryRepository;
    private final InquiryRepository inquiryRepository;

    public InquiryQueryService(
            InquiryQueryRepository inquiryQueryRepository,
            InquiryRepository inquiryRepository
    ) {
        this.inquiryQueryRepository = inquiryQueryRepository;
        this.inquiryRepository = inquiryRepository;
    }

    @Override
    public InquiryPage getPage(int page, int pageSize) {
        long totalCount = inquiryQueryRepository.countAll();
        int totalPages = totalCount == 0 ? 1 : (int) Math.ceil((double) totalCount / pageSize);
        int safePage = Math.max(1, Math.min(page, totalPages));
        int offset = (safePage - 1) * pageSize;

        List<InquirySummary> items = inquiryQueryRepository.findPage(offset, pageSize).stream()
                .map(r -> new InquirySummary(r.id(), r.title(), r.viewCount(), r.createdAt()))
                .toList();

        return new InquiryPage(items, safePage, totalPages, totalCount);
    }

    @Override
    public InquiryDetail getDetail(Long id) {
        InquiryQueryResult result = inquiryRepository.findById(id)
                .orElseThrow(() -> new InquiryNotFoundException(id));

        inquiryRepository.incrementViewCount(id);

        return new InquiryDetail(
                result.id(),
                result.authorName(),
                result.title(),
                result.content(),
                result.viewCount() + 1L,
                result.createdAt()
        );
    }
}
