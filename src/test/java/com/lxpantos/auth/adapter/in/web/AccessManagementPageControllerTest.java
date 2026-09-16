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
    void memberPageIsAdminOnlyAndShowsAdminSidebar() throws Exception {
        // ADMIN 은 회원 관리 화면에 접근할 수 있고, 회원 관리·권한 관리·문의 요청 메뉴가 보인다.
        mockMvc.perform(get("/members")
                        .session(sessionFor(adminId, "page-test-admin@example.com", MemberRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("href=\"/members\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("href=\"/admin/access\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("href=\"/inquiries\"")));
    }

    @Test
    void memberPageForbiddenForViewer() throws Exception {
        // OPERATOR 이하(여기서는 VIEWER)는 회원 관리 화면에 접근하면 403 을 받는다.
        mockMvc.perform(get("/members")
                        .session(sessionFor(viewerId, "page-test-viewer@example.com", MemberRole.VIEWER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void viewerReceivesForbiddenOnAccessPage() throws Exception {
        mockMvc.perform(get("/admin/access")
                        .session(sessionFor(viewerId, "page-test-viewer@example.com", MemberRole.VIEWER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void inquiryPagesKeepRoleAwareSidebar() throws Exception {
        jdbcTemplate.update("INSERT INTO inquiries (member_id, title, content, created_at) VALUES (?, ?, ?, ?)",
                adminId, "Sidebar test", "Content", LocalDateTime.now());
        Long inquiryId = jdbcTemplate.queryForObject(
                "SELECT id FROM inquiries WHERE member_id = ?", Long.class, adminId);
        for (String path : new String[]{"/inquiries", "/inquiries/new", "/inquiries/" + inquiryId}) {
            for (MemberRole role : new MemberRole[]{MemberRole.ADMIN, MemberRole.VIEWER}) {
                Long id = role == MemberRole.ADMIN ? adminId : viewerId;
                var response = mockMvc.perform(get(path).session(sessionFor(id, "page-test@example.com", role)))
                        .andExpect(status().isOk());
                String html = response.andReturn().getResponse().getContentAsString();
                String sidebar = html.substring(html.indexOf("<aside"), html.indexOf("</aside>"));
                org.assertj.core.api.Assertions.assertThat(sidebar.contains("href=\"/admin/access\""))
                        .as("admin menu on %s for %s", path, role).isEqualTo(role == MemberRole.ADMIN);
                // 회원 관리 메뉴도 ADMIN 에게만 노출된다.
                org.assertj.core.api.Assertions.assertThat(sidebar.contains("href=\"/members\""))
                        .as("member menu on %s for %s", path, role).isEqualTo(role == MemberRole.ADMIN);
                org.assertj.core.api.Assertions.assertThat(sidebar)
                        .containsPattern("(?s)<a[^>]*href=\"/inquiries\"[^>]*aria-current=\"page\"");
            }
        }
    }

    @Test
    void unauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/access"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
