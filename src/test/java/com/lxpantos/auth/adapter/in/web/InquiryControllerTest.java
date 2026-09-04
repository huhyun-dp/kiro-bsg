package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.exception.InquiryNotFoundException;
import com.lxpantos.auth.application.port.in.CreateInquiryUseCase;
import com.lxpantos.auth.application.port.in.InquiryDetail;
import com.lxpantos.auth.application.port.in.InquiryPage;
import com.lxpantos.auth.application.port.in.InquiryQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryControllerTest {

    private final SessionMember sessionMember = new SessionMember(1L, "test@example.com", "홍길동");

    private CreateInquiryUseCase createUseCase;
    private InquiryQueryUseCase queryUseCase;
    private InquiryController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER, sessionMember);
    }

    @Test
    void listReturnsInquiryListView() {
        InquiryPage page = new InquiryPage(List.of(), 1, 1, 0L);
        queryUseCase = new InquiryQueryUseCase() {
            @Override
            public InquiryPage getPage(int p, int ps) { return page; }
            @Override
            public InquiryDetail getDetail(Long id) { return null; }
        };
        createUseCase = cmd -> 1L;
        controller = new InquiryController(createUseCase, queryUseCase);

        Model model = new ConcurrentModel();
        String view = controller.list(1, session, model);

        assertThat(view).isEqualTo("inquiry/list");
        assertThat(model.getAttribute("inquiryPage")).isEqualTo(page);
    }

    @Test
    void newFormReturnsFormView() {
        queryUseCase = new InquiryQueryUseCase() {
            @Override
            public InquiryPage getPage(int p, int ps) { return new InquiryPage(List.of(), 1, 1, 0L); }
            @Override
            public InquiryDetail getDetail(Long id) { return null; }
        };
        createUseCase = cmd -> 1L;
        controller = new InquiryController(createUseCase, queryUseCase);

        Model model = new ConcurrentModel();
        String view = controller.newForm(session, model);

        assertThat(view).isEqualTo("inquiry/form");
    }

    @Test
    void detailReturnsDetailView() {
        InquiryDetail detail = new InquiryDetail(
                1L, "홍길동", "제목", "내용", 1L, LocalDateTime.now());
        queryUseCase = new InquiryQueryUseCase() {
            @Override
            public InquiryPage getPage(int p, int ps) { return new InquiryPage(List.of(), 1, 1, 0L); }
            @Override
            public InquiryDetail getDetail(Long id) { return detail; }
        };
        createUseCase = cmd -> 1L;
        controller = new InquiryController(createUseCase, queryUseCase);

        Model model = new ConcurrentModel();
        String view = controller.detail(1L, session, model);

        assertThat(view).isEqualTo("inquiry/detail");
        assertThat(model.getAttribute("inquiry")).isEqualTo(detail);
    }

    @Test
    void detailThrows404WhenInquiryNotFound() {
        queryUseCase = new InquiryQueryUseCase() {
            @Override
            public InquiryPage getPage(int p, int ps) { return new InquiryPage(List.of(), 1, 1, 0L); }
            @Override
            public InquiryDetail getDetail(Long id) { throw new InquiryNotFoundException(id); }
        };
        createUseCase = cmd -> 1L;
        controller = new InquiryController(createUseCase, queryUseCase);

        Model model = new ConcurrentModel();
        assertThatThrownBy(() -> controller.detail(999L, session, model))
                .hasMessageContaining("999");
    }
}
