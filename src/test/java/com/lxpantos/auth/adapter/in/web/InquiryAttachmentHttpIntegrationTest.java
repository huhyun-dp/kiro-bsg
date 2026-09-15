package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.domain.member.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InquiryAttachmentHttpIntegrationTest {
    private static final byte[] PDF = "%PDF-1.7 attachment".getBytes();

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long memberId;
    private Path storageRoot;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.update("DELETE FROM inquiry_attachments");
        jdbcTemplate.update("DELETE FROM inquiries");
        jdbcTemplate.update("DELETE FROM members WHERE email = ?", "attachment-http@example.com");
        jdbcTemplate.update("INSERT INTO members (email, password_hash, name, phone_number, created_at, role, status, version) VALUES (?, ?, ?, ?, ?, 'VIEWER', 'ACTIVE', 0)",
                "attachment-http@example.com", "$2a$04$abcdefghijklmnopqrstuv", "첨부테스트", "01000000000", LocalDateTime.now());
        memberId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE email = ?", Long.class, "attachment-http@example.com");
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
    void authenticatedMultipartCreatePersistsMetadataAndStreamsSecureDownload() throws Exception {
        CreatedAttachment created = createAttachment();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inquiry_attachments WHERE inquiry_id = ?", Integer.class, created.inquiryId())).isEqualTo(1);
        assertThat(Files.readAllBytes(storageRoot.resolve("files").resolve(created.storageKey()))).isEqualTo(PDF);

        mockMvc.perform(get("/inquiries/{inquiryId}/attachments/{attachmentId}/download", created.inquiryId(), created.attachmentId())
                        .session(authenticatedSession()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void uploadRequiresCsrfAndDownloadRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/inquiries")
                        .file(pdfUpload())
                        .param("title", "첨부 문의")
                        .param("content", "첨부 파일을 확인해 주세요.")
                        .session(authenticatedSession()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/inquiries/1/attachments/1/download"))
                .andExpect(status().is3xxRedirection());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inquiries", Integer.class)).isZero();
    }

    @Test
    void missingFileAndInquiryAttachmentMismatchBothReturnNotFound() throws Exception {
        CreatedAttachment created = createAttachment();
        jdbcTemplate.update("INSERT INTO inquiries (member_id, title, content, view_count, created_at, deleted) VALUES (?, ?, ?, 0, ?, FALSE)",
                memberId, "다른 문의", "내용", LocalDateTime.now());
        Long otherInquiryId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM inquiries", Long.class);

        mockMvc.perform(get("/inquiries/{inquiryId}/attachments/{attachmentId}/download", otherInquiryId, created.attachmentId())
                        .session(authenticatedSession()))
                .andExpect(status().isNotFound());
        Files.delete(storageRoot.resolve("files").resolve(created.storageKey()));
        mockMvc.perform(get("/inquiries/{inquiryId}/attachments/{attachmentId}/download", created.inquiryId(), created.attachmentId())
                        .session(authenticatedSession()))
                .andExpect(status().isNotFound());
    }

    private CreatedAttachment createAttachment() throws Exception {
        mockMvc.perform(multipart("/inquiries")
                        .file(pdfUpload())
                        .param("title", "첨부 문의")
                        .param("content", "첨부 파일을 확인해 주세요.")
                        .param("_csrf", "csrf-token")
                        .session(authenticatedSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/inquiries/*"));
        Long inquiryId = jdbcTemplate.queryForObject("SELECT id FROM inquiries WHERE member_id = ? ORDER BY id DESC LIMIT 1", Long.class, memberId);
        return jdbcTemplate.queryForObject("SELECT inquiry_id, id, storage_key FROM inquiry_attachments WHERE inquiry_id = ?", (resultSet, rowNum) ->
                new CreatedAttachment(resultSet.getLong("inquiry_id"), resultSet.getLong("id"), resultSet.getString("storage_key")), inquiryId);
    }

    private MockMultipartFile pdfUpload() {
        return new MockMultipartFile("attachments", "proof.pdf", "application/pdf", PDF);
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER, new SessionMember(memberId, "attachment-http@example.com", "첨부테스트", MemberRole.VIEWER));
        session.setAttribute(SessionKeys.CSRF_TOKEN, "csrf-token");
        return session;
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }

    private record CreatedAttachment(Long inquiryId, Long attachmentId, String storageKey) { }
}
