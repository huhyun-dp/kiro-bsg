package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    private final HomeController controller = new HomeController();

    @Test
    void redirectsAuthenticatedMemberToMemberManagement() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(
                SessionKeys.AUTHENTICATED_MEMBER,
                new SessionMember(1L, "user@example.com", "홍길동")
        );

        assertThat(controller.home(request)).isEqualTo("redirect:/members");
    }

    @Test
    void redirectsAnonymousUserToLogin() {
        assertThat(controller.home(new MockHttpServletRequest())).isEqualTo("redirect:/login");
    }
}
