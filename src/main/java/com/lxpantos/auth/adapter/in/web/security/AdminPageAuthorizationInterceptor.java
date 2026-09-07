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

/**
 * 관리자 전용 화면(/admin/**) 접근 제어.
 * - 미인증: 로그인 화면으로 이동
 * - 정지된 회원: 세션 무효화 후 로그인 화면으로 이동
 * - 비관리자: HTTP 403
 * 권한은 세션이 아닌 최신 DB 상태로 판단한다.
 */
@Component
public class AdminPageAuthorizationInterceptor implements HandlerInterceptor {

    private final MemberAccessLookupUseCase memberAccessLookupUseCase;

    public AdminPageAuthorizationInterceptor(MemberAccessLookupUseCase memberAccessLookupUseCase) {
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

        Optional<CurrentMemberAccess> current = memberAccessLookupUseCase.findAccessById(member.id());
        if (current.isEmpty() || !current.get().status().isActive()) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if (!current.get().role().isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.");
            return false;
        }
        return true;
    }
}
