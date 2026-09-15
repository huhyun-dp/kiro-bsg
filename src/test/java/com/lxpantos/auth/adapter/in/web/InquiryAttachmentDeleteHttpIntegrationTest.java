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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP integration coverage for the immediate per-attachment delete endpoint (task 9.10):
 * author immediate delete removes metadata and file then redirects to the edit screen,
 * non-author 403, unknown/mismatched attachment 404, CSRF missing rejected, unauthenticated redirect.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InquiryAttachmentDeleteHttpIntegrationTest {
    private static final byte[] PDF = "%PDF-1.7 attachment".getBytes();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long ownerId;
    private Long otherMemberId;
    private Path storageRoot;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.update("DELETE FROM inquiry_attachments");
        jdbcTemplate.update("DELETE FROM inquiries");
        jdbcTemplate.update("DELETE FROM members WHERE email IN (?, ?)", "attachment-delete-owner@example.com", "attachment-delete-other@example.com");
        ownerId = insertMember("attachment-delete-owner@example.com", "작성자");
        otherMemberId = insertMember("attachment-delete-other@example.com", "타인");
        storageRoot = Path.of(System.getProperty("java.io.tmpdir"), "session-auth-test-attachments");
        if (Files.exists(storageRoot)) {
            try (var paths = Files.walk(storageRoot)) {
                paths.sorted(Comparator.reverseOrder()).filter(path -> !path.equals(storageRoot)).forEach(this::deleteQuietly);
            }
        }
        Files.createDirectories(storageRoot.resolve("staging"));
        Files.createDirectories(storageRoot.resolve("files"));
    }

    @Test
    void authorImmediateDeleteRemovesMetadataAndFileThenRedirectsToEdit() throws Exception {
        CreatedAttachment created = createAttachment();
        assertThat(Files.exists(storageRoot.resolve("files").resolve(created.storageKey()))).isTrue();

        mockMvc.perform(post("/inquiries/{id}/attachments/{attachmentId}/delete", created.inquiryId(), created.attachmentId())
                        .param("_csrf", "csrf-token")
                        .session(authenticatedSession(ownerId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inquiries/" + created.inquiryId() + "/edit"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inquiry_attachments WHERE id = ?", Integer.class, created.attachmentId())).isZero();
        assertThat(Files.exists(storageRoot.resolve("files").resolve(created.storageKey()))).isFalse();
    }

    @Test
    void nonAuthorReceivesForbiddenAndNothingIsRemoved() throws Exception {
        CreatedAttachment created = createAttachment();

        mockMvc.perform(post("/inquiries/{id}/attachments/{attachmentId}/delete", created.inquiryId(), created.attachmentId())
                        .param("_csrf", "csrf-token")
                        .session(authenticatedSession(otherMemberId)))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inquiry_attachments WHERE id = ?", Integer.class, created.attachmentId())).isEqualTo(1);
        assertThat(Files.exists(storageRoot.resolve("files").resolve(created.storageKey()))).isTrue();
    }

    @Test
    void unknownAndMismatchedAttachmentReturnNotFound() throws Exception {
        CreatedAttachment created = createAttachment();

        // Unknown attachment id on the owner's inquiry.
        mockMvc.perform(post("/inquiries/{id}/attachments/{attachmentId}/delete", created.inquiryId(), 999999L)
                        .param("_csrf", "csrf-token")
                        .session(authenticatedSession(ownerId)))
                .andExpect(status().isNotFound());

        // Attachment exists but belongs to a different inquiry owned by the same author.
        jdbcTemplate.update("INSERT INTO inquiries (member_id, title, content, view_count, created_at, deleted) VALUES (?, ?, ?, 0, ?, FALSE)",
                ownerId, "다른 문의", "내용", LocalDateTime.now());
        Long otherInquiryId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM inquiries", Long.class);

        mockMvc.perform(post("/inquiries/{id}/attachments/{attachmentId}/delete", otherInquiryId, created.attachmentId())
                        .param("_csrf", "csrf-token")
                        .session(authenticatedSession(ownerId)))
                .andExpect(status().isNotFound());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inquiry_attachments WHERE id = ?", Integer.class, created.attachmentId())).isEqualTo(1);
    }

    @Test
    void missingCsrfTokenIsRejected() throws Exception {
        CreatedAttachment created = createAttachment();

        mockMvc.perform(post("/inquiries/{id}/attachments/{attachmentId}/delete", created.inquiryId(), created.attachmentId())
                        .session(authenticatedSession(ownerId)))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inquiry_attachments WHERE id = ?", Integer.class, created.attachmentId())).isEqualTo(1);
        assertThat(Files.exists(storageRoot.resolve("files").resolve(created.storageKey()))).isTrue();
    }

    @Test
    void unauthenticatedRequestRedirectsToLogin() throws Exception {
        CreatedAttachment created = createAttachment();

        // Session carries a valid CSRF token but no authenticated member, so CSRF passes and
        // the authentication interceptor redirects to /login.
        MockHttpSession unauthenticated = new MockHttpSession();
        unauthenticated.setAttribute(SessionKeys.CSRF_TOKEN, "csrf-token");

        mockMvc.perform(post("/inquiries/{id}/attachments/{attachmentId}/delete", created.inquiryId(), created.attachmentId())
                        .param("_csrf", "csrf-token")
                        .session(unauthenticated))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inquiry_attachments WHERE id = ?", Integer.class, created.attachmentId())).isEqualTo(1);
    }

    private CreatedAttachment createAttachment() throws Exception {
        mockMvc.perform(multipart("/inquiries")
                        .file(new MockMultipartFile("attachments", "proof.pdf", "application/pdf", PDF))
                        .param("title", "첨부 문의")
                        .param("content", "첨부 파일을 확인해 주세요.")
                        .param("_csrf", "csrf-token")
                        .session(authenticatedSession(ownerId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/inquiries/*"));
        Long inquiryId = jdbcTemplate.queryForObject("SELECT id FROM inquiries WHERE member_id = ? ORDER BY id DESC LIMIT 1", Long.class, ownerId);
        return jdbcTemplate.queryForObject("SELECT inquiry_id, id, storage_key FROM inquiry_attachments WHERE inquiry_id = ?", (resultSet, rowNum) ->
                new CreatedAttachment(resultSet.getLong("inquiry_id"), resultSet.getLong("id"), resultSet.getString("storage_key")), inquiryId);
    }

    private Long insertMember(String email, String name) {
        jdbcTemplate.update("INSERT INTO members (email, password_hash, name, phone_number, created_at, role, status, version) VALUES (?, ?, ?, ?, ?, 'VIEWER', 'ACTIVE', 0)",
                email, "$2a$04$abcdefghijklmnopqrstuv", name, "01000000000", LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT id FROM members WHERE email = ?", Long.class, email);
    }

    private MockHttpSession authenticatedSession(Long memberId) {
        String email = memberId.equals(ownerId) ? "attachment-delete-owner@example.com" : "attachment-delete-other@example.com";
        String name = memberId.equals(ownerId) ? "작성자" : "타인";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER, new SessionMember(memberId, email, name, MemberRole.VIEWER));
        session.setAttribute(SessionKeys.CSRF_TOKEN, "csrf-token");
        return session;
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }

    private record CreatedAttachment(Long inquiryId, Long attachmentId, String storageKey) { }
}
