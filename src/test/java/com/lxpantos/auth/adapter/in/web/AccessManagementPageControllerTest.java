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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessManagementPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long adminId;
    private Long viewerId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM members WHERE email LIKE 'page-test-%'");
        adminId = insert("page-test-admin@example.com", "관리자", "ADMIN");
        viewerId = insert("page-test-viewer@example.com", "뷰어", "VIEWER");
    }

    private Long insert(String email, String name, String role) {
        jdbcTemplate.update(
                "INSERT INTO members (email, password_hash, name, phone_number, created_at, role, status, version) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 0)",
                email, "$2a$04$abcdefghijklmnopqrstuv", name, "01000000000", LocalDateTime.now(), role);
        return jdbcTemplate.queryForObject("SELECT id FROM members WHERE email = ?", Long.class, email);
    }

    private MockHttpSession sessionFor(Long id, String email, MemberRole role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER, new SessionMember(id, email, "회원", role));
        return session;
    }

    @Test
    void adminCanOpenAccessPage() throws Exception {
        mockMvc.perform(get("/admin/access")
                        .session(sessionFor(adminId, "page-test-admin@example.com", MemberRole.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void viewerReceivesForbiddenOnAccessPage() throws Exception {
        mockMvc.perform(get("/admin/access")
                        .session(sessionFor(viewerId, "page-test-viewer@example.com", MemberRole.VIEWER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/access"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
