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
public class ApiAuthenticationInterceptor implements HandlerInterceptor {

    private static final String UNAUTHORIZED_BODY = "{\"message\":\"로그인이 필요합니다.\"}";

    private final MemberAccessLookupUseCase memberAccessLookupUseCase;

    public ApiAuthenticationInterceptor(MemberAccessLookupUseCase memberAccessLookupUseCase) {
        this.memberAccessLookupUseCase = memberAccessLookupUseCase;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        SessionMember member = session == null
                ? null
                : (SessionMember) session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER);

        if (member == null) {
            writeUnauthorized(response);
            return false;
        }

        Optional<CurrentMemberAccess> current = memberAccessLookupUseCase.findAccessById(member.id());
        if (current.isEmpty() || !current.get().status().isActive()) {
            session.invalidate();
            writeUnauthorized(response);
            return false;
        }
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(UNAUTHORIZED_BODY);
    }
}
