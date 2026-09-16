package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.form.LoginForm;
import com.lxpantos.auth.application.port.in.AuthenticatedMember;
import com.lxpantos.auth.application.port.in.LoginUseCase;
import com.lxpantos.auth.application.port.in.RegisterMemberUseCase;
import com.lxpantos.auth.domain.member.MemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    @Test
    void redirectsToInquiriesAfterSuccessfulLogin() {
        // 회원 관리 화면은 ADMIN 전용이 되었으므로, 로그인 성공 후에는 모든 회원이 이용 가능한
        // 문의 요청 화면으로 이동한다.
        RegisterMemberUseCase registerMemberUseCase = command -> 1L;
        LoginUseCase loginUseCase = command -> new AuthenticatedMember(1L, command.email(), "홍길동", MemberRole.VIEWER);
        AuthController controller = new AuthController(registerMemberUseCase, loginUseCase);
        LoginForm form = new LoginForm();
        form.setEmail("user@example.com");
        form.setPassword("password1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession();

        String view = controller.login(form, new BeanPropertyBindingResult(form, "loginForm"), request);

        assertThat(view).isEqualTo("redirect:/inquiries");
    }
}
