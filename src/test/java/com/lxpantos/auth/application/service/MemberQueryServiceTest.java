package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.port.in.MemberSummary;
import com.lxpantos.auth.application.port.out.MemberQueryResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemberQueryServiceTest {

    @Test
    void masksPhoneNumbersBeforeReturningMemberSummaries() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 24, 10, 0);
        MemberQueryService service = new MemberQueryService(keyword -> List.of(
                new MemberQueryResult(1L, "홍길동", "hong@example.com", "01012345678", createdAt, null),
                new MemberQueryResult(2L, "김회원", "kim@example.com", null, createdAt, createdAt),
                new MemberQueryResult(3L, "이회원", "lee@example.com", "invalid", createdAt, null)
        ));

        List<MemberSummary> members = service.search(null);

        assertThat(members).extracting(MemberSummary::maskedPhoneNumber)
                .containsExactly("010-****-5678", "미등록", "****");
        assertThat(members.get(1).lastLoginAt()).isEqualTo(createdAt);
    }

    @Test
    void masksEmailBeforeReturningMemberSummaries() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 24, 10, 0);
        MemberQueryService service = new MemberQueryService(keyword -> List.of(
                // 일반 케이스: 앞 1자리 노출, 나머지 로컬 파트 마스킹
                new MemberQueryResult(1L, "홍길동", "hong@example.com", "01012345678", createdAt, null),
                // 로컬 파트가 1자리인 경우: 마스킹할 부분 없음, 그대로 반환
                new MemberQueryResult(2L, "김회원", "a@example.com", null, createdAt, null),
                // 로컬 파트가 2자리인 경우: 앞 1자리만 노출, 1자리 마스킹
                new MemberQueryResult(3L, "이회원", "ab@example.com", null, createdAt, null)
        ));

        List<MemberSummary> members = service.search(null);

        assertThat(members).extracting(MemberSummary::email)
                .containsExactly("h***@example.com", "a@example.com", "a*@example.com");
    }
}
