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

import java.io.IOException;
import java.util.Optional;

/**
 * 관리자 전용 API(/api/admin/**) 접근 제어.
 * - 미인증: HTTP 401 JSON
 * - 정지된 회원: 세션 무효화 후 HTTP 401 JSON
 * - 비관리자: HTTP 403 JSON
 * 권한은 세션이 아닌 최신 DB 상태로 판단한다.
 */
@Component
public class AdminApiAuthorizationInterceptor implements HandlerInterceptor {

    private static final String UNAUTHORIZED_BODY = "{\"message\":\"로그인이 필요합니다.\"}";
    private static final String FORBIDDEN_BODY = "{\"message\":\"접근 권한이 없습니다.\"}";

    private final MemberAccessLookupUseCase memberAccessLookupUseCase;

    public AdminApiAuthorizationInterceptor(MemberAccessLookupUseCase memberAccessLookupUseCase) {
        this.memberAccessLookupUseCase = memberAccessLookupUseCase;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        SessionMember member = session == null
                ? null
                : (SessionMember) session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER);

        if (member == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_BODY);
            return false;
        }

        Optional<CurrentMemberAccess> current = memberAccessLookupUseCase.findAccessById(member.id());
        if (current.isEmpty() || !current.get().status().isActive()) {
            session.invalidate();
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_BODY);
            return false;
        }
        if (!current.get().role().isAdmin()) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_BODY);
            return false;
        }
        return true;
    }

    private void writeJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(body);
    }
}
