package com.lxpantos.auth.config;

import com.lxpantos.auth.application.port.out.AccessMemberRepository;
import com.lxpantos.auth.application.port.out.AuditLogEntry;
import com.lxpantos.auth.application.port.out.TransactionRunner;
import com.lxpantos.auth.domain.member.Member;
import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapAdminInitializerTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-24T01:02:03Z"), ZoneId.of("Asia/Seoul"));

    private final FakeRepo repo = new FakeRepo();
    private final TransactionRunner tx = new TransactionRunner() {
        @Override
        public <T> T execute(Supplier<T> action) {
            return action.get();
        }
    };

    @Test
    void promotesConfiguredEmailToActiveAdmin() {
        repo.members.put("boss@example.com",
                member(1L, "boss@example.com", MemberRole.VIEWER, MemberStatus.ACTIVE));

        new BootstrapAdminInitializer(repo, tx, CLOCK, "  BOSS@Example.com ").run(null);

        assertThat(repo.updatedRole).isEqualTo(MemberRole.ADMIN);
        assertThat(repo.updatedStatus).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void doesNothingWhenEmailBlank() {
        new BootstrapAdminInitializer(repo, tx, CLOCK, "").run(null);
        assertThat(repo.updateCalls).isZero();
    }

    @Test
    void skipsWhenAlreadyActiveAdmin() {
        repo.members.put("boss@example.com",
                member(1L, "boss@example.com", MemberRole.ADMIN, MemberStatus.ACTIVE));

        new BootstrapAdminInitializer(repo, tx, CLOCK, "boss@example.com").run(null);

        assertThat(repo.updateCalls).isZero();
    }

    @Test
    void skipsWhenMemberMissing() {
        new BootstrapAdminInitializer(repo, tx, CLOCK, "missing@example.com").run(null);
        assertThat(repo.updateCalls).isZero();
    }

    private Member member(Long id, String email, MemberRole role, MemberStatus status) {
        return new Member(id, email, "hash", "이름", "01000000000",
                LocalDateTime.now(CLOCK), null, role, status, 0L, null);
    }

    private static final class FakeRepo implements AccessMemberRepository {
        private final Map<String, Member> members = new HashMap<>();
        private int updateCalls = 0;
        private MemberRole updatedRole;
        private MemberStatus updatedStatus;

        @Override
        public Optional<Member> findById(Long memberId) {
            return Optional.empty();
        }

        @Override
        public Optional<Member> findByEmail(String email) {
            return Optional.ofNullable(members.get(email));
        }

        @Override
        public int updateAccess(Long memberId, MemberRole role, MemberStatus status,
                                LocalDateTime roleUpdatedAt, long expectedVersion) {
            updateCalls++;
            updatedRole = role;
            updatedStatus = status;
            return 1;
        }

        @Override
        public int updateAccessKeepingLastAdmin(Long memberId, MemberRole role, MemberStatus status,
                                                LocalDateTime roleUpdatedAt, long expectedVersion) {
            return updateAccess(memberId, role, status, roleUpdatedAt, expectedVersion);
        }

        @Override
        public void saveAuditLog(AuditLogEntry entry) {
        }
    }
}
