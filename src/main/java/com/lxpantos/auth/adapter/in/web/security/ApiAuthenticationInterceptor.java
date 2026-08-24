package com.lxpantos.auth.adapter.in.web.security;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiAuthenticationInterceptor implements HandlerInterceptor {

    private static final String UNAUTHORIZED_BODY = "{\"message\":\"로그인이 필요합니다.\"}";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER) != null) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(UNAUTHORIZED_BODY);
        return false;
    }
}
