package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.form.LoginForm;
import com.lxpantos.auth.application.port.in.AuthenticatedMember;
import com.lxpantos.auth.application.port.in.LoginUseCase;
import com.lxpantos.auth.application.port.in.RegisterMemberUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    @Test
    void redirectsToMemberManagementAfterSuccessfulLogin() {
        RegisterMemberUseCase registerMemberUseCase = command -> 1L;
        LoginUseCase loginUseCase = command -> new AuthenticatedMember(1L, command.email(), "홍길동");
        AuthController controller = new AuthController(registerMemberUseCase, loginUseCase);
        LoginForm form = new LoginForm();
        form.setEmail("user@example.com");
        form.setPassword("password1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession();

        String view = controller.login(form, new BeanPropertyBindingResult(form, "loginForm"), request);

        assertThat(view).isEqualTo("redirect:/members");
    }
}
