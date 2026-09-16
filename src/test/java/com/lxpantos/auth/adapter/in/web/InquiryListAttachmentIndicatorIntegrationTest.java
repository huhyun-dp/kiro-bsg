package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.adapter.in.web.session.SessionKeys;
import com.lxpantos.auth.adapter.in.web.session.SessionMember;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryQueryRepository;
import com.lxpantos.auth.domain.member.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InquiryListAttachmentIndicatorIntegrationTest {
    private static final String STORAGE_KEY = "11111111-2222-3333-4444-555555555555";
    private static final String PRIVATE_PATH = "/private/inquiry/evidence.pdf";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @SpyBean private InquiryQueryRepository inquiryQueryRepository;
    @SpyBean private InquiryAttachmentQueryRepository attachmentQueryRepository;

    private Long memberId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM inquiry_attachments");
        jdbcTemplate.update("DELETE FROM inquiries");
        jdbcTemplate.update("DELETE FROM members WHERE email = ?", "inquiry-list@example.com");
        jdbcTemplate.update("INSERT INTO members (email, password_hash, name, phone_number, created_at, role, status, version) VALUES (?, ?, ?, ?, ?, 'VIEWER', 'ACTIVE', 0)",
                "inquiry-list@example.com", "$2a$04$abcdefghijklmnopqrstuv", "목록테스트", "01000000000", LocalDateTime.now());
        memberId = jdbcTemplate.queryForObject("SELECT id FROM members WHERE email = ?", Long.class, "inquiry-list@example.com");
        reset(inquiryQueryRepository, attachmentQueryRepository);
    }

    @Test
    void mixedRowsRenderAccessibleIndicatorOnlyForAttachedInquiryWithoutAttachmentMetadataExposure() throws Exception {
        Long attachedInquiryId = insertInquiry("첨부 문의", LocalDateTime.of(2024, 6, 2, 10, 0));
        insertInquiry("일반 문의", LocalDateTime.of(2024, 6, 1, 10, 0));
        jdbcTemplate.update("INSERT INTO inquiry_attachments (inquiry_id, storage_key, original_filename, content_type, file_size, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                attachedInquiryId, STORAGE_KEY, PRIVATE_PATH, "application/pdf", 128L, LocalDateTime.now());

        MvcResult result = mockMvc.perform(get("/inquiries").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(countOccurrences(content, "첨부파일")).isEqualTo(1);
        assertThat(countOccurrences(content, "inquiry-attachment-indicator")).isEqualTo(1);
        // 첨부가 1개인 문의는 개수와 함께 "첨부파일(1)" 로 표시된다.
        assertThat(content).contains("첨부파일(1)");
        assertThat(content).contains("첨부 문의", "일반 문의");
        assertThat(content).doesNotContain(STORAGE_KEY, PRIVATE_PATH, "/attachments/", "download", "storage_key");
        verify(inquiryQueryRepository).countAll();
        verify(inquiryQueryRepository).findPage(0, 10);
        verifyNoInteractions(attachmentQueryRepository);
    }

    @Test
    void attachmentIndicatorShowsFileCountWithoutMetadataExposure() throws Exception {
        Long attachedInquiryId = insertInquiry("첨부 여러개", LocalDateTime.of(2024, 6, 3, 10, 0));
        for (int i = 0; i < 3; i++) {
            jdbcTemplate.update("INSERT INTO inquiry_attachments (inquiry_id, storage_key, original_filename, content_type, file_size, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    attachedInquiryId, java.util.UUID.randomUUID().toString(), PRIVATE_PATH, "application/pdf", 128L, LocalDateTime.now());
        }

        MvcResult result = mockMvc.perform(get("/inquiries").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        // 첨부 3개는 "첨부파일(3)" 으로 표시된다.
        assertThat(content).contains("첨부파일(3)");
        assertThat(countOccurrences(content, "inquiry-attachment-indicator")).isEqualTo(1);
        // 개수만 노출하고 저장 키·경로·다운로드 주소는 노출하지 않는다.
        assertThat(content).doesNotContain(PRIVATE_PATH, "/attachments/", "download", "storage_key");
        // 행별 첨부 조회 없이 단일 목록 쿼리로 개수를 계산한다.
        verify(inquiryQueryRepository).findPage(0, 10);
        verifyNoInteractions(attachmentQueryRepository);
    }

    @Test
    void emptyListRendersExistingEmptyStateWithoutAttachmentIndicator() throws Exception {
        MvcResult result = mockMvc.perform(get("/inquiries").session(authenticatedSession()))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("등록된 문의가 없습니다.");
        assertThat(content).doesNotContain("첨부파일", "inquiry-attachment-indicator", "/attachments/", "download");
        verify(inquiryQueryRepository).countAll();
        verify(inquiryQueryRepository).findPage(0, 10);
        verifyNoInteractions(attachmentQueryRepository);
    }

    private Long insertInquiry(String title, LocalDateTime createdAt) {
        jdbcTemplate.update("INSERT INTO inquiries (member_id, title, content, view_count, created_at, deleted) VALUES (?, ?, ?, 0, ?, FALSE)",
                memberId, title, "내용", createdAt);
        return jdbcTemplate.queryForObject("SELECT id FROM inquiries WHERE member_id = ? AND title = ?", Long.class, memberId, title);
    }

    private MockHttpSession authenticatedSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.AUTHENTICATED_MEMBER,
                new SessionMember(memberId, "inquiry-list@example.com", "목록테스트", MemberRole.VIEWER));
        return session;
    }

    private int countOccurrences(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
