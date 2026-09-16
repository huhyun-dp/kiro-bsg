package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.InquiryDetail;
import com.lxpantos.auth.application.port.out.InquiryListQueryResult;
import com.lxpantos.auth.application.port.out.InquiryQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.domain.inquiry.Inquiry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryQueryServiceViewCountTest {

    private static final Long INQUIRY_ID = 10L;
    private static final Long AUTHOR_ID = 42L;
    private static final long STORED_VIEW_COUNT = 7L;

    private FakeInquiryRepository repository;
    private InquiryQueryService service;

    @BeforeEach
    void setUp() {
        repository = new FakeInquiryRepository();
        service = new InquiryQueryService(new EmptyQueryRepository(), repository);
    }

    @Test
    void doesNotIncrementViewCountWhenAuthorViewsOwnInquiry() {
        // 작성자 본인이 조회하면 조회수를 증가시키지 않고 저장값을 그대로 반환한다.
        InquiryDetail detail = service.getDetail(INQUIRY_ID, AUTHOR_ID);

        assertThat(repository.incrementCalls).isZero();
        assertThat(detail.viewCount()).isEqualTo(STORED_VIEW_COUNT);
    }

    @Test
    void incrementsViewCountWhenNonAuthorViews() {
        InquiryDetail detail = service.getDetail(INQUIRY_ID, 999L);

        assertThat(repository.incrementCalls).isEqualTo(1);
        assertThat(detail.viewCount()).isEqualTo(STORED_VIEW_COUNT + 1L);
    }

    @Test
    void incrementsViewCountWhenViewerIsUnknown() {
        // 하위호환: 조회자 미상(null)이면 기존처럼 1 증가한다.
        InquiryDetail detail = service.getDetail(INQUIRY_ID, null);

        assertThat(repository.incrementCalls).isEqualTo(1);
        assertThat(detail.viewCount()).isEqualTo(STORED_VIEW_COUNT + 1L);
    }

    @Test
    void legacyGetDetailWithoutViewerIncrementsViewCount() {
        // 하위호환 오버로드도 조회수를 1 증가시킨다.
        InquiryDetail detail = service.getDetail(INQUIRY_ID);

        assertThat(repository.incrementCalls).isEqualTo(1);
        assertThat(detail.viewCount()).isEqualTo(STORED_VIEW_COUNT + 1L);
    }

    @Test
    void throwsNotFoundWhenInquiryMissing() {
        repository.missing = true;

        assertThatThrownBy(() -> service.getDetail(INQUIRY_ID, AUTHOR_ID))
                .isInstanceOf(InquiryNotFoundException.class);
        assertThat(repository.incrementCalls).isZero();
    }

    // ── Fakes ─────────────────────────────────────────────

    private static final class FakeInquiryRepository implements InquiryRepository {
        int incrementCalls;
        boolean missing;

        @Override public Long save(Inquiry inquiry) { return 1L; }

        @Override public void incrementViewCount(Long id) { incrementCalls++; }

        @Override public Optional<InquiryQueryResult> findById(Long id) {
            if (missing) return Optional.empty();
            return Optional.of(new InquiryQueryResult(id, "작성자", "제목", "내용", STORED_VIEW_COUNT,
                    LocalDateTime.of(2024, 1, 1, 0, 0)));
        }

        @Override public Optional<Long> findOwnerId(Long id) {
            if (missing) return Optional.empty();
            return Optional.of(AUTHOR_ID);
        }

        @Override public int updateContent(Long id, String title, String content) { return 1; }

        @Override public int softDelete(Long id) { return 1; }
    }

    private static final class EmptyQueryRepository implements InquiryQueryRepository {
        @Override public List<InquiryListQueryResult> findPage(int offset, int limit) { return List.of(); }
        @Override public long countAll() { return 0; }
    }
}
