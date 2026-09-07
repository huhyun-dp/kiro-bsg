package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.domain.member.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessManagementApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long adminId;
    private Long viewerId;
    private Long operatorId;
    private Long secondAdminId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM member_access_audit_log");
        jdbcTemplate.update("DELETE FROM members WHERE email LIKE 'access-test-%'");
        // 시드/이전 테스트가 만든 역할을 정리해 이 테스트가 관리자 모집단을 완전히 통제하게 한다.
        jdbcTemplate.update("UPDATE members SET role = 'VIEWER', status = 'ACTIVE'");
        adminId = insert("access-test-admin@example.com", "관리자", "01000000001", "ADMIN", "ACTIVE");
        secondAdminId = insert("access-test-admin2@example.com", "관리자2", "01000000002", "ADMIN", "ACTIVE");
        operatorId = insert("access-test-op@example.com", "운영자", "01000000003", "OPERATOR", "ACTIVE");
        viewerId = insert("access-test-viewer@example.com", "뷰어", "01000000004", "VIEWER", "ACTIVE");
    }

    private Long insert(String email, String name, String phone, String role, String status) {
        jdbcTemplate.update(
                "INSERT INTO members (email, password_hash, name, phone_number, created_at, role, status, version) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 0)",
                email, "$2a$04$abcdefghijklmnopqrstuv", name, phone, LocalDateTime.now(), role, status);
        return jdbcTemplate.queryForObject("SELECT id FROM members WHERE email = ?", Long.class, email);
    }

    private MockHttpSession sessionFor(Long id, String email, String name, MemberRole role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER, new SessionMember(id, email, name, role));
        session.setAttribute(SessionKeys.CSRF_TOKEN, "test-token");
        return session;
    }

    private MockHttpSession adminSession() {
        return sessionFor(adminId, "access-test-admin@example.com", "관리자", MemberRole.ADMIN);
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void viewerRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .session(sessionFor(viewerId, "access-test-viewer@example.com", "뷰어", MemberRole.VIEWER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorRequestReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .session(sessionFor(operatorId, "access-test-op@example.com", "운영자", MemberRole.OPERATOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSearchReturnsMembersWithMaskedPhone() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .param("keyword", "access-test-viewer")
                        .session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].maskedPhoneNumber").value("010-****-0004"));
    }

    @Test
    void adminSearchAppliesRoleFilter() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .param("role", "ADMIN")
                        .session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void adminSearchAppliesStatusFilterAndPagination() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .param("keyword", "access-test")
                        .param("status", "ACTIVE")
                        .param("size", "2")
                        .param("page", "0")
                        .session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void adminChangesRoleAndStatusSuccessfully() throws Exception {
        mockMvc.perform(put("/api/admin/members/" + viewerId + "/access")
                        .session(adminSession())
                        .header("X-CSRF-TOKEN", "test-token")
                        .contentType("application/json")
                        .content("{\"role\":\"OPERATOR\",\"status\":\"ACTIVE\",\"reason\":\"운영자 승격\",\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.version").value(1));

        String role = jdbcTemplate.queryForObject("SELECT role FROM members WHERE id = ?", String.class, viewerId);
        Integer logs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member_access_audit_log WHERE target_member_id = ?", Integer.class, viewerId);
        org.assertj.core.api.Assertions.assertThat(role).isEqualTo("OPERATOR");
        org.assertj.core.api.Assertions.assertThat(logs).isEqualTo(1);
    }

    @Test
    void changeWithShortReasonReturns400() throws Exception {
        mockMvc.perform(put("/api/admin/members/" + viewerId + "/access")
                        .session(adminSession())
                        .header("X-CSRF-TOKEN", "test-token")
                        .contentType("application/json")
                        .content("{\"role\":\"OPERATOR\",\"status\":\"ACTIVE\",\"reason\":\"짧음\",\"expectedVersion\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void selfDowngradeReturns400() throws Exception {
        mockMvc.perform(put("/api/admin/members/" + adminId + "/access")
                        .session(adminSession())
                        .header("X-CSRF-TOKEN", "test-token")
                        .contentType("application/json")
                        .content("{\"role\":\"VIEWER\",\"status\":\"ACTIVE\",\"reason\":\"본인 강등 시도\",\"expectedVersion\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanDemoteAnotherAdminWhenAnotherActiveAdminRemains() throws Exception {
        // 활성 관리자가 2명이므로 다른 관리자를 강등하면 성공한다(마지막 관리자 보호 규칙은
        // 서비스 단위 테스트에서 검증한다).
        mockMvc.perform(put("/api/admin/members/" + secondAdminId + "/access")
                        .session(adminSession())
                        .header("X-CSRF-TOKEN", "test-token")
                        .contentType("application/json")
                        .content("{\"role\":\"VIEWER\",\"status\":\"ACTIVE\",\"reason\":\"권한 회수\",\"expectedVersion\":0}"))
                .andExpect(status().isOk());
    }

    @Test
    void versionConflictReturns409() throws Exception {
        mockMvc.perform(put("/api/admin/members/" + viewerId + "/access")
                        .session(adminSession())
                        .header("X-CSRF-TOKEN", "test-token")
                        .contentType("application/json")
                        .content("{\"role\":\"OPERATOR\",\"status\":\"ACTIVE\",\"reason\":\"동시 수정 충돌\",\"expectedVersion\":99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void changeForMissingMemberReturns404() throws Exception {
        mockMvc.perform(put("/api/admin/members/99999999/access")
                        .session(adminSession())
                        .header("X-CSRF-TOKEN", "test-token")
                        .contentType("application/json")
                        .content("{\"role\":\"OPERATOR\",\"status\":\"ACTIVE\",\"reason\":\"없는 회원\",\"expectedVersion\":0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeWithoutCsrfTokenReturns403() throws Exception {
        mockMvc.perform(put("/api/admin/members/" + viewerId + "/access")
                        .session(adminSession())
                        .contentType("application/json")
                        .content("{\"role\":\"OPERATOR\",\"status\":\"ACTIVE\",\"reason\":\"CSRF 없음\",\"expectedVersion\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLogsReturnedNewestFirst() throws Exception {
        String token = "test-token";
        MockHttpSession session = adminSession();
        mockMvc.perform(put("/api/admin/members/" + viewerId + "/access")
                        .session(session).header("X-CSRF-TOKEN", token).contentType("application/json")
                        .content("{\"role\":\"OPERATOR\",\"status\":\"ACTIVE\",\"reason\":\"첫번째 변경\",\"expectedVersion\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/members/" + viewerId + "/access")
                        .session(session).header("X-CSRF-TOKEN", token).contentType("application/json")
                        .content("{\"role\":\"VIEWER\",\"status\":\"SUSPENDED\",\"reason\":\"두번째 변경\",\"expectedVersion\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/members/" + viewerId + "/audit-logs")
                        .param("size", "1")
                        .session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].reason").value("두번째 변경"))
                .andExpect(jsonPath("$.content[0].maskedRequestIp").isNotEmpty());
    }

    @Test
    void suspendedMemberSessionIsBlockedOnNextRequest() throws Exception {
        MockHttpSession viewerSession =
                sessionFor(viewerId, "access-test-viewer@example.com", "뷰어", MemberRole.VIEWER);
        // 회원 목록 API 는 정상 (VIEWER 도 /api/members 는 접근 가능)
        mockMvc.perform(get("/api/members").session(viewerSession))
                .andExpect(status().isOk());

        // 관리자가 해당 회원을 정지시킨다.
        jdbcTemplate.update("UPDATE members SET status = 'SUSPENDED' WHERE id = ?", viewerId);

        // 기존 세션의 다음 요청은 차단된다(401).
        mockMvc.perform(get("/api/members").session(viewerSession))
                .andExpect(status().isUnauthorized());
    }
}
