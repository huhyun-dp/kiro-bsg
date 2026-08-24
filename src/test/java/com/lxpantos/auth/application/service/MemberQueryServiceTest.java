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
}
