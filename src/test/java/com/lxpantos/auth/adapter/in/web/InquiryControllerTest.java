package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.exception.InquiryAccessDeniedException;
import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.CreateInquiryCommand;
import com.lxpantos.auth.application.port.in.CreateInquiryUseCase;
import com.lxpantos.auth.application.port.in.DeleteInquiryCommand;
import com.lxpantos.auth.application.port.in.DeleteInquiryUseCase;
import com.lxpantos.auth.application.port.in.InquiryDetail;
import com.lxpantos.auth.application.port.in.InquiryPage;
import com.lxpantos.auth.application.port.in.InquiryQueryUseCase;
import com.lxpantos.auth.application.port.in.UpdateInquiryCommand;
import com.lxpantos.auth.application.port.in.UpdateInquiryUseCase;
import com.lxpantos.auth.domain.member.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
    private InquiryController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        queryUseCase = new FakeQuery();
        updateUseCase = new RecordingUpdate();
        deleteUseCase = new RecordingDelete();
        controller = new InquiryController(cmd -> 1L, updateUseCase, deleteUseCase, queryUseCase);
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
    void detailCanManageIsFalseForNonOwnerViewer() {
        queryUseCase.detail = detailOf(1L);
        queryUseCase.ownerId = 2L; // 작성자 != viewer(1)
        Model model = new ConcurrentModel();

        controller.detail(1L, session, model);

        assertThat(model.getAttribute("canManage")).isEqualTo(false);
    }

    @Test
    void detailCanManageIsTrueForAdminEvenIfNotOwner() {
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER, admin);
        queryUseCase.detail = detailOf(1L);
        queryUseCase.ownerId = 2L;
        Model model = new ConcurrentModel();

        controller.detail(1L, session, model);

        assertThat(model.getAttribute("canManage")).isEqualTo(true);
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
    }

    @Test
    void updateMaps403WhenAccessDenied() {
        updateUseCase.throwAccessDenied = true;
        Model model = new ConcurrentModel();
        BindingResult noErrors = new BeanPropertyBindingResult(new Object(), "inquiryForm");
        var form = new com.lxpantos.auth.adapter.in.web.form.InquiryForm();
        form.setTitle("제목"); form.setContent("내용");

        assertThatThrownBy(() -> controller.update(1L, form, noErrors, session, model))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
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

    // ── Fakes ─────────────────────────────────────────────

    private static final class FakeQuery implements InquiryQueryUseCase {
        InquiryPage page = new InquiryPage(List.of(), 1, 1, 0L);
        InquiryDetail detail;
        Long ownerId;
        boolean notFound;

        @Override public InquiryPage getPage(int p, int ps) { return page; }
        @Override public InquiryDetail getDetail(Long id) {
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
            return com.lxpantos.auth.application.port.in.UpdateInquiryResult.withoutDeletedAttachments(command.inquiryId());
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
}
