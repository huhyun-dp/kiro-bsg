package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.form.ChangeAccessRequest;
import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.port.in.AccessAuditLogPage;
import com.lxpantos.auth.application.port.in.AccessManagementUseCase;
import com.lxpantos.auth.application.port.in.AccessMemberPage;
import com.lxpantos.auth.application.port.in.AccessMemberSearchQuery;
import com.lxpantos.auth.application.port.in.AccessMemberSummary;
import com.lxpantos.auth.application.port.in.ChangeMemberAccessCommand;
import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/members")
public class AccessManagementApiController {

    private final AccessManagementUseCase accessManagementUseCase;

    public AccessManagementApiController(AccessManagementUseCase accessManagementUseCase) {
        this.accessManagementUseCase = accessManagementUseCase;
    }

    @GetMapping
    public AccessMemberPage search(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "role", required = false) MemberRole role,
            @RequestParam(name = "status", required = false) MemberStatus status,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        return accessManagementUseCase.searchMembers(
                new AccessMemberSearchQuery(keyword, role, status, page, size)
        );
    }

    @PutMapping("/{memberId}/access")
    public AccessMemberSummary changeAccess(
            @PathVariable("memberId") Long memberId,
            @Valid @RequestBody ChangeAccessRequest request,
            HttpSession session,
            HttpServletRequest httpRequest
    ) {
        SessionMember actor = (SessionMember) session.getAttribute(SessionKeys.AUTHENTICATED_MEMBER);
        return accessManagementUseCase.changeAccess(new ChangeMemberAccessCommand(
                actor.id(),
                memberId,
                request.getRole(),
                request.getStatus(),
                request.getReason(),
                request.getExpectedVersion(),
                clientIp(httpRequest)
        ));
    }

    @GetMapping("/{memberId}/audit-logs")
    public AccessAuditLogPage auditLogs(
            @PathVariable("memberId") Long memberId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        return accessManagementUseCase.auditLogs(memberId, page, size);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
