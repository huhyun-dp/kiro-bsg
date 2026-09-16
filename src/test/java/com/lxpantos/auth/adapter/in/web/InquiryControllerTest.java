package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.exception.InquiryAccessDeniedException;
import com.lxpantos.auth.application.exception.InquiryAttachmentNotFoundException;
import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.CreateInquiryCommand;
import com.lxpantos.auth.application.port.in.CreateInquiryUseCase;
import com.lxpantos.auth.application.port.in.DeleteInquiryAttachmentCommand;
import com.lxpantos.auth.application.port.in.DeleteInquiryAttachmentUseCase;
import com.lxpantos.auth.application.port.in.DeleteInquiryCommand;
import com.lxpantos.auth.application.port.in.DeleteInquiryUseCase;
import com.lxpantos.auth.application.port.in.InquiryAttachmentSummary;
import com.lxpantos.auth.application.port.in.InquiryDetail;
import com.lxpantos.auth.application.port.in.InquiryPage;
import com.lxpantos.auth.application.port.in.InquiryQueryUseCase;
import com.lxpantos.auth.application.port.in.UpdateInquiryCommand;
import com.lxpantos.auth.application.port.in.UpdateInquiryUseCase;
import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;
import com.lxpantos.auth.domain.member.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryControllerTest {

    private final SessionMember viewer = new SessionMember(1L, "viewer@example.com", "홍길동", MemberRole.VIEWER);
    private final SessionMember admin = new SessionMember(9L, "admin@example.com", "관리자", MemberRole.ADMIN);

    private FakeQuery queryUseCase;
    private RecordingUpdate updateUseCase;
    private RecordingDelete deleteUseCase;
    private RecordingDeleteAttachment deleteAttachmentUseCase;
    private InquiryController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        queryUseCase = new FakeQuery();
        updateUseCase = new RecordingUpdate();
        deleteUseCase = new RecordingDelete();
        deleteAttachmentUseCase = new RecordingDeleteAttachment();
        controller = new InquiryController(cmd -> 1L, updateUseCase, deleteUseCase, deleteAttachmentUseCase,
                queryUseCase, new InquiryAttachmentUploadValidator(), null, null);
        session = new MockHttpSession();
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER, viewer);
    }

    @Test
    void listReturnsInquiryListView() {
        InquiryPage page = new InquiryPage(List.of(), 1, 1, 0L);
        queryUseCase.page = page;
        Model model = new ConcurrentModel();

        String view = controller.list(1, session, model);

        assertThat(view).isEqualTo("inquiry/list");
        assertThat(model.getAttribute("inquiryPage")).isEqualTo(page);
    }

    @Test
    void detailReturnsDetailViewAndExposesCanManageForOwner() {
        queryUseCase.detail = detailOf(1L);
        queryUseCase.ownerId = 1L; // 작성자 = viewer(1)
        Model model = new ConcurrentModel();

        String view = controller.detail(1L, session, model);

        assertThat(view).isEqualTo("inquiry/detail");
        assertThat(model.getAttribute("canManage")).isEqualTo(true);
    }

    @Test
    void detailPassesSessionMemberIdAsViewerForSelfCountPrevention() {
        // 조회수 셀프 카운트 방지를 위해 상세 조회 시 세션 회원 ID를 조회자로 전달한다.
        queryUseCase.detail = detailOf(1L);
        queryUseCase.ownerId = 1L;
        Model model = new ConcurrentModel();

        controller.detail(1L, session, model);

        assertThat(queryUseCase.lastDetailViewerIdSet).isTrue();
        assertThat(queryUseCase.lastDetailViewerId).isEqualTo(viewer.id());
    }

    @Test
    void detailCanManageIsFalseForNonOwnerViewer() {
        queryUseCase.detail = detailOf(1L);
        queryUseCase.ownerId = 2L; // 작성자 != viewer(1)
        Model model = new ConcurrentModel();

        controller.detail(1L, session, model);

        assertThat(model.getAttribute("canManage")).isEqualTo(false);
    }

    @Test
    void detailCanManageIsFalseForAdminWhoIsNotAuthor() {
        // Inquiry editing/attachment management is author-only (Requirement 5.16 / Q4).
        // An ADMIN who is not the author cannot manage, so canManage must be false.
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER, admin);
        queryUseCase.detail = detailOf(1L);
        queryUseCase.ownerId = 2L; // 작성자 != admin(9)
        Model model = new ConcurrentModel();

        controller.detail(1L, session, model);

        assertThat(model.getAttribute("canManage")).isEqualTo(false);
    }

    @Test
    void detailThrows404WhenInquiryNotFound() {
        queryUseCase.notFound = true;
        Model model = new ConcurrentModel();

        assertThatThrownBy(() -> controller.detail(999L, session, model))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("999");
    }

    @Test
    void editFormForbiddenForNonOwnerViewer() {
        queryUseCase.detail = detailOf(1L);
        queryUseCase.ownerId = 2L;
        Model model = new ConcurrentModel();

        assertThatThrownBy(() -> controller.editForm(1L, session, model))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void updateRedirectsToDetailOnSuccess() {
        // The update path first resolves the author via findOwnerId, then reads the current
        // detail through getDetailWithoutView to compute the retained-attachment baseline.
        queryUseCase.ownerId = 1L; // 작성자 = viewer(1)
        queryUseCase.detail = detailOf(1L);
        Model model = new ConcurrentModel();
        BindingResult noErrors = new BeanPropertyBindingResult(new Object(), "inquiryForm");
        var form = new com.lxpantos.auth.adapter.in.web.form.InquiryForm();
        form.setTitle("수정 제목");
        form.setContent("수정 내용");

        String view = controller.update(1L, form, noErrors, session, model);

        assertThat(view).isEqualTo("redirect:/inquiries/1");
        assertThat(updateUseCase.lastCommand.inquiryId()).isEqualTo(1L);
        assertThat(updateUseCase.lastCommand.actorMemberId()).isEqualTo(1L);
        assertThat(updateUseCase.lastCommand.actorAdmin()).isFalse();
        // Edit POST no longer carries a deletion list; only new attachments (none here).
        assertThat(updateUseCase.lastCommand.newAttachments()).isEmpty();
    }

    @Test
    void updateForbiddenForNonAuthorBeforeUseCaseIsInvoked() {
        // Author-only rule is enforced in the controller (requireAuthor) before the update
        // use case runs, so a non-author viewer is rejected with 403 and no update happens.
        queryUseCase.ownerId = 2L; // 작성자 != viewer(1)
        queryUseCase.detail = detailOf(1L);
        Model model = new ConcurrentModel();
        BindingResult noErrors = new BeanPropertyBindingResult(new Object(), "inquiryForm");
        var form = new com.lxpantos.auth.adapter.in.web.form.InquiryForm();
        form.setTitle("제목"); form.setContent("내용");

        assertThatThrownBy(() -> controller.update(1L, form, noErrors, session, model))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        assertThat(updateUseCase.lastCommand).isNull();
    }

    @Test
    void updateMaps403WhenUseCaseReportsAccessDenied() {
        // Defense in depth: even if the controller-level author check passes, an access-denied
        // signal from the use case is mapped to 403.
        queryUseCase.ownerId = 1L; // controller-level author check passes
        queryUseCase.detail = detailOf(1L);
        updateUseCase.throwAccessDenied = true;
        Model model = new ConcurrentModel();
        BindingResult noErrors = new BeanPropertyBindingResult(new Object(), "inquiryForm");
        var form = new com.lxpantos.auth.adapter.in.web.form.InquiryForm();
        form.setTitle("제목"); form.setContent("내용");

        assertThatThrownBy(() -> controller.update(1L, form, noErrors, session, model))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // ── 개별 첨부 즉시 삭제 (POST /inquiries/{id}/attachments/{attachmentId}/delete) ──

    @Test
    void deleteAttachmentRedirectsToEditOnSuccess() {
        queryUseCase.ownerId = 1L; // 작성자 = viewer(1)

        String view = controller.deleteAttachment(1L, 5L, session);

        assertThat(view).isEqualTo("redirect:/inquiries/1/edit");
        assertThat(deleteAttachmentUseCase.lastCommand.inquiryId()).isEqualTo(1L);
        assertThat(deleteAttachmentUseCase.lastCommand.attachmentId()).isEqualTo(5L);
        assertThat(deleteAttachmentUseCase.lastCommand.actorMemberId()).isEqualTo(1L);
    }

    @Test
    void deleteAttachmentForbiddenForNonAuthorViewer() {
        queryUseCase.ownerId = 2L; // 작성자 != viewer(1)

        assertThatThrownBy(() -> controller.deleteAttachment(1L, 5L, session))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        assertThat(deleteAttachmentUseCase.lastCommand).isNull();
    }

    @Test
    void deleteAttachmentReturns404WhenInquiryMissing() {
        queryUseCase.ownerId = null; // 문의 없음

        assertThatThrownBy(() -> controller.deleteAttachment(999L, 5L, session))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        assertThat(deleteAttachmentUseCase.lastCommand).isNull();
    }

    @Test
    void deleteAttachmentMaps404WhenAttachmentDoesNotBelong() {
        queryUseCase.ownerId = 1L;
        deleteAttachmentUseCase.throwNotFound = true;

        assertThatThrownBy(() -> controller.deleteAttachment(1L, 5L, session))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void deleteAttachmentMaps403WhenUseCaseReportsAccessDenied() {
        queryUseCase.ownerId = 1L; // controller-level author check passes
        deleteAttachmentUseCase.throwAccessDenied = true;

        assertThatThrownBy(() -> controller.deleteAttachment(1L, 5L, session))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // ── 편집 POST 는 더 이상 삭제하지 않음: 현재 저장분 + 새 파일 기준 5개/20 MiB 경계 ──

    @Test
    void updateAcceptsWhenRetainedPlusNewCountEqualsFive() {
        // 현재 저장분 4개 + 새 파일 1개 = 5개 (경계 허용)
        queryUseCase.ownerId = 1L;
        queryUseCase.detail = detailWithAttachments(1L, 4, 1L);
        Model model = new ConcurrentModel();
        var form = formWithNewAttachments(pngUpload("a.png", 100L));

        String view = controller.update(1L, form, freshBinding(), session, model);

        assertThat(view).isEqualTo("redirect:/inquiries/1");
        assertThat(updateUseCase.lastCommand.newAttachments()).hasSize(1);
    }

    @Test
    void updateRejectsWhenRetainedPlusNewCountExceedsFive() {
        // 현재 저장분 5개 + 새 파일 1개 = 6개 (초과)
        queryUseCase.ownerId = 1L;
        queryUseCase.detail = detailWithAttachments(1L, 5, 1L);
        Model model = new ConcurrentModel();
        var form = formWithNewAttachments(pngUpload("a.png", 100L));

        String view = controller.update(1L, form, freshBinding(), session, model);

        // Validation failure keeps the edit form and does not invoke the update use case.
        assertThat(view).isEqualTo("inquiry/form");
        assertThat(updateUseCase.lastCommand).isNull();
    }

    @Test
    void updateRejectsWhenSingleNewFileExceedsTenMiB() {
        queryUseCase.ownerId = 1L;
        queryUseCase.detail = detailWithAttachments(1L, 0, 1L);
        Model model = new ConcurrentModel();
        long tooBig = 10L * 1024 * 1024 + 1;
        var form = formWithNewAttachments(pngUpload("big.png", tooBig));

        String view = controller.update(1L, form, freshBinding(), session, model);

        assertThat(view).isEqualTo("inquiry/form");
        assertThat(updateUseCase.lastCommand).isNull();
    }

    @Test
    void updateRejectsWhenRetainedPlusNewSizeExceedsTwentyMiB() {
        // 현재 저장분 15 MiB + 새 파일 6 MiB = 21 MiB (초과)
        queryUseCase.ownerId = 1L;
        long retainedSize = 15L * 1024 * 1024;
        queryUseCase.detail = detailWithAttachmentSizes(1L, List.of(retainedSize));
        Model model = new ConcurrentModel();
        var form = formWithNewAttachments(pngUpload("new.png", 6L * 1024 * 1024));

        String view = controller.update(1L, form, freshBinding(), session, model);

        assertThat(view).isEqualTo("inquiry/form");
        assertThat(updateUseCase.lastCommand).isNull();
    }

    @Test
    void updateAcceptsWhenRetainedPlusNewSizeEqualsTwentyMiB() {
        // 현재 저장분 15 MiB + 새 파일 5 MiB = 20 MiB (경계 허용)
        queryUseCase.ownerId = 1L;
        long retainedSize = 15L * 1024 * 1024;
        queryUseCase.detail = detailWithAttachmentSizes(1L, List.of(retainedSize));
        Model model = new ConcurrentModel();
        var form = formWithNewAttachments(pngUpload("new.png", 5L * 1024 * 1024));

        String view = controller.update(1L, form, freshBinding(), session, model);

        assertThat(view).isEqualTo("redirect:/inquiries/1");
        assertThat(updateUseCase.lastCommand.newAttachments()).hasSize(1);
    }

    @Test
    void deleteRedirectsToListOnSuccess() {
        String view = controller.delete(1L, session);

        assertThat(view).isEqualTo("redirect:/inquiries");
        assertThat(deleteUseCase.lastCommand.inquiryId()).isEqualTo(1L);
        assertThat(deleteUseCase.lastCommand.actorMemberId()).isEqualTo(1L);
    }

    @Test
    void deleteMaps403WhenAccessDenied() {
        deleteUseCase.throwAccessDenied = true;

        assertThatThrownBy(() -> controller.delete(1L, session))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    private InquiryDetail detailOf(Long id) {
        return new InquiryDetail(id, "홍길동", "제목", "내용", 1L, LocalDateTime.now());
    }

    private InquiryDetail detailWithAttachments(Long id, int count, long eachSize) {
        List<InquiryAttachmentSummary> attachments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            attachments.add(new InquiryAttachmentSummary((long) (i + 1), "file" + i + ".pdf", AttachmentMediaType.PDF, eachSize));
        }
        return new InquiryDetail(id, "홍길동", "제목", "내용", 1L, LocalDateTime.now(), attachments);
    }

    private InquiryDetail detailWithAttachmentSizes(Long id, List<Long> sizes) {
        List<InquiryAttachmentSummary> attachments = new ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) {
            attachments.add(new InquiryAttachmentSummary((long) (i + 1), "file" + i + ".pdf", AttachmentMediaType.PDF, sizes.get(i)));
        }
        return new InquiryDetail(id, "홍길동", "제목", "내용", 1L, LocalDateTime.now(), attachments);
    }

    private BindingResult freshBinding() {
        return new BeanPropertyBindingResult(new Object(), "inquiryForm");
    }

    private com.lxpantos.auth.adapter.in.web.form.InquiryForm formWithNewAttachments(MultipartFile... files) {
        var form = new com.lxpantos.auth.adapter.in.web.form.InquiryForm();
        form.setTitle("수정 제목");
        form.setContent("수정 내용");
        form.setAttachments(List.of(files));
        return form;
    }

    /**
     * Builds a PNG upload whose declared type and 8-byte signature pass the upload validator,
     * while padding the body so {@code getSize()} reports the requested logical size. The
     * validator relies on {@code getSize()} for the boundary checks, so the array itself does
     * not need to be gigabytes large.
     */
    private MultipartFile pngUpload(String filename, long reportedSize) {
        byte[] signature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        return new MockMultipartFile("attachments", filename, "image/png", signature) {
            @Override public long getSize() { return reportedSize; }
        };
    }

    // ── Fakes ─────────────────────────────────────────────

    private static final class FakeQuery implements InquiryQueryUseCase {
        InquiryPage page = new InquiryPage(List.of(), 1, 1, 0L);
        InquiryDetail detail;
        Long ownerId;
        Long lastDetailViewerId;
        boolean lastDetailViewerIdSet;
        boolean notFound;

        @Override public InquiryPage getPage(int p, int ps) { return page; }
        @Override public InquiryDetail getDetail(Long id, Long viewerMemberId) {
            this.lastDetailViewerId = viewerMemberId;
            this.lastDetailViewerIdSet = true;
            if (notFound) throw new InquiryNotFoundException(id);
            return detail;
        }
        @Override public InquiryDetail getDetailWithoutView(Long id) {
            if (notFound) throw new InquiryNotFoundException(id);
            return detail;
        }
        @Override public Optional<Long> findOwnerId(Long id) { return Optional.ofNullable(ownerId); }
    }

    private static final class RecordingUpdate implements UpdateInquiryUseCase {
        UpdateInquiryCommand lastCommand;
        boolean throwAccessDenied;
        @Override public com.lxpantos.auth.application.port.in.UpdateInquiryResult update(UpdateInquiryCommand command) {
            this.lastCommand = command;
            if (throwAccessDenied) throw new InquiryAccessDeniedException();
            return com.lxpantos.auth.application.port.in.UpdateInquiryResult.of(command.inquiryId());
        }
    }

    private static final class RecordingDelete implements DeleteInquiryUseCase {
        DeleteInquiryCommand lastCommand;
        boolean throwAccessDenied;
        @Override public void delete(DeleteInquiryCommand command) {
            this.lastCommand = command;
            if (throwAccessDenied) throw new InquiryAccessDeniedException();
        }
    }

    private static final class RecordingDeleteAttachment implements DeleteInquiryAttachmentUseCase {
        DeleteInquiryAttachmentCommand lastCommand;
        boolean throwAccessDenied;
        boolean throwNotFound;
        @Override public void deleteAttachment(DeleteInquiryAttachmentCommand command) {
            this.lastCommand = command;
            if (throwNotFound) throw new InquiryAttachmentNotFoundException();
            if (throwAccessDenied) throw new InquiryAccessDeniedException();
        }
    }
}
