package com.lxpantos.auth.adapter.in.web.security;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

@Component
public class CsrfTokenFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTRIBUTE = "_csrf";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(SAFE_METHODS.contains(request.getMethod()));
        String expectedToken = session == null ? null : (String) session.getAttribute(SessionKeys.CSRF_TOKEN);

        if (expectedToken == null && SAFE_METHODS.contains(request.getMethod())) {
            expectedToken = newToken();
            session.setAttribute(SessionKeys.CSRF_TOKEN, expectedToken);
        }

        if (!SAFE_METHODS.contains(request.getMethod()) && !tokenMatches(expectedToken, submittedToken(request))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "요청을 확인할 수 없습니다. 페이지를 새로고침해 주세요.");
            return;
        }

        request.setAttribute(REQUEST_ATTRIBUTE, expectedToken);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/css/") || path.startsWith("/fonts/") || path.equals("/favicon.ico");
    }

    private String submittedToken(HttpServletRequest request) {
        String headerToken = request.getHeader("X-CSRF-TOKEN");
        return headerToken != null ? headerToken : request.getParameter(REQUEST_ATTRIBUTE);
    }

    private boolean tokenMatches(String expected, String submitted) {
        if (expected == null || submitted == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

