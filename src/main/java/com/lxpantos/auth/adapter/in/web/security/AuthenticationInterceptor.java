package com.lxpantos.auth.adapter.in.web.security;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.port.in.CurrentMemberAccess;
import com.lxpantos.auth.application.port.in.MemberAccessLookupUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final MemberAccessLookupUseCase memberAccessLookupUseCase;

    public AuthenticationInterceptor(MemberAccessLookupUseCase memberAccessLookupUseCase) {
        this.memberAccessLookupUseCase = memberAccessLookupUseCase;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        SessionMember member = session == null
                ? null
                : (SessionMember) session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER);

        if (member == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        // 로그인 이후 정지된 회원은 다음 요청에서 세션을 무효화하고 차단한다.
        Optional<CurrentMemberAccess> current = memberAccessLookupUseCase.findAccessById(member.id());
        if (current.isEmpty() || !current.get().status().isActive()) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        return true;
    }
}
