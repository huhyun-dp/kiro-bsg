package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.form.LoginForm;
import com.lxpantos.auth.adapter.in.web.form.SignUpForm;
import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.exception.DuplicateEmailException;
import com.lxpantos.auth.application.exception.InvalidCredentialsException;
import com.lxpantos.auth.application.port.in.AuthenticatedMember;
import com.lxpantos.auth.application.port.in.LoginCommand;
import com.lxpantos.auth.application.port.in.LoginUseCase;
import com.lxpantos.auth.application.port.in.RegisterMemberCommand;
import com.lxpantos.auth.application.port.in.RegisterMemberUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class AuthController {

    private final RegisterMemberUseCase registerMemberUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(RegisterMemberUseCase registerMemberUseCase, LoginUseCase loginUseCase) {
        this.registerMemberUseCase = registerMemberUseCase;
        this.loginUseCase = loginUseCase;
    }

    @GetMapping("/login")
    public String loginForm(HttpServletRequest request, Model model) {
        if (isAuthenticated(request)) {
            return "redirect:/members";
        }
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(
            @Valid LoginForm loginForm,
            BindingResult bindingResult,
            HttpServletRequest request
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            AuthenticatedMember member = loginUseCase.login(
                    new LoginCommand(loginForm.getEmail(), loginForm.getPassword())
            );

            request.changeSessionId();
            HttpSession session = request.getSession();
            session.setAttribute(
                    SessionKeys.AUTHENTICATED_MEMBER,
                    new SessionMember(member.id(), member.email(), member.name())
            );
            return "redirect:/members";
        } catch (InvalidCredentialsException exception) {
            bindingResult.reject("login.failed", exception.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/signup")
    public String signUpForm(HttpServletRequest request, Model model) {
        if (isAuthenticated(request)) {
            return "redirect:/members";
        }
        if (!model.containsAttribute("signUpForm")) {
            model.addAttribute("signUpForm", new SignUpForm());
        }
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signUp(
            @Valid SignUpForm signUpForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (!Objects.equals(signUpForm.getPassword(), signUpForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "비밀번호가 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            registerMemberUseCase.register(new RegisterMemberCommand(
                    signUpForm.getEmail(),
                    signUpForm.getPassword(),
                    signUpForm.getName(),
                    signUpForm.getPhoneNumber()
            ));
        } catch (DuplicateEmailException exception) {
            bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
            return "auth/signup";
        }

        redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("successMessage", "로그아웃되었습니다.");
        return "redirect:/login";
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER) != null;
    }
}
