package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.InquiryAccessDeniedException;
import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.CreateInquiryCommand;
import com.lxpantos.auth.application.port.in.DeleteInquiryCommand;
import com.lxpantos.auth.application.port.in.UpdateInquiryCommand;
import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.domain.inquiry.Inquiry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2024-06-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private FakeInquiryRepository repository;
    private InquiryService service;

    @BeforeEach
    void setUp() {
        repository = new FakeInquiryRepository();
        service = new InquiryService(repository, fixedClock);
    }

    @Test
    void savesInquiryWithCorrectFields() {
        Long returnedId = service.create(new CreateInquiryCommand(42L, "테스트 제목", "테스트 내용"));

        Inquiry saved = repository.lastSaved;
        assertThat(returnedId).isEqualTo(1L);
        assertThat(saved.memberId()).isEqualTo(42L);
        assertThat(saved.title()).isEqualTo("테스트 제목");
        assertThat(saved.content()).isEqualTo("테스트 내용");
        assertThat(saved.viewCount()).isZero();
        assertThat(saved.createdAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }

    // ── 수정 권한 ──────────────────────────────────────────

    @Test
    void ownerCanUpdate() {
        repository.ownerId = 42L;
        service.update(new UpdateInquiryCommand(1L, 42L, false, "새 제목", "새 내용"));

        assertThat(repository.updatedTitle).isEqualTo("새 제목");
        assertThat(repository.updatedContent).isEqualTo("새 내용");
    }

    @Test
    void adminCanUpdateOthersInquiry() {
        repository.ownerId = 42L;
        service.update(new UpdateInquiryCommand(1L, 99L, true, "관리자 수정", "내용"));

        assertThat(repository.updatedTitle).isEqualTo("관리자 수정");
    }

    @Test
    void nonOwnerNonAdminCannotUpdate() {
        repository.ownerId = 42L;
        assertThatThrownBy(() ->
                service.update(new UpdateInquiryCommand(1L, 7L, false, "x", "y")))
                .isInstanceOf(InquiryAccessDeniedException.class);
        assertThat(repository.updatedTitle).isNull();
    }

    @Test
    void updateMissingInquiryThrowsNotFound() {
        repository.ownerId = null; // 조회 안 됨
        assertThatThrownBy(() ->
                service.update(new UpdateInquiryCommand(404L, 1L, false, "x", "y")))
                .isInstanceOf(InquiryNotFoundException.class);
    }

    // ── 삭제 권한 ──────────────────────────────────────────

    @Test
    void ownerCanDelete() {
        repository.ownerId = 42L;
        service.delete(new DeleteInquiryCommand(1L, 42L, false));
        assertThat(repository.softDeleted).isTrue();
    }

    @Test
    void adminCanDeleteOthersInquiry() {
        repository.ownerId = 42L;
        service.delete(new DeleteInquiryCommand(1L, 99L, true));
        assertThat(repository.softDeleted).isTrue();
    }

    @Test
    void nonOwnerNonAdminCannotDelete() {
        repository.ownerId = 42L;
        assertThatThrownBy(() ->
                service.delete(new DeleteInquiryCommand(1L, 7L, false)))
                .isInstanceOf(InquiryAccessDeniedException.class);
        assertThat(repository.softDeleted).isFalse();
    }

    @Test
    void deleteMissingInquiryThrowsNotFound() {
        repository.ownerId = null;
        assertThatThrownBy(() ->
                service.delete(new DeleteInquiryCommand(404L, 1L, false)))
                .isInstanceOf(InquiryNotFoundException.class);
    }

    // ── Fake ──────────────────────────────────────────────

    private static final class FakeInquiryRepository implements InquiryRepository {
        long idSequence = 1;
        Inquiry lastSaved;
        Long ownerId;
        String updatedTitle;
        String updatedContent;
        boolean softDeleted;

        @Override
        public Long save(Inquiry inquiry) {
            long id = idSequence++;
            lastSaved = new Inquiry(id, inquiry.memberId(), inquiry.title(),
                    inquiry.content(), inquiry.viewCount(), inquiry.createdAt());
            return id;
        }

        @Override public void incrementViewCount(Long id) { }
        @Override public Optional<InquiryQueryResult> findById(Long id) { return Optional.empty(); }
        @Override public Optional<Long> findOwnerId(Long id) { return Optional.ofNullable(ownerId); }

        @Override
        public int updateContent(Long id, String title, String content) {
            updatedTitle = title;
            updatedContent = content;
            return 1;
        }

        @Override
        public int softDelete(Long id) {
            softDeleted = true;
            return 1;
        }
    }
}
