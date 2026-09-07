package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.AccessRuleViolationException;
import com.lxpantos.auth.application.exception.MemberNotFoundException;
import com.lxpantos.auth.application.exception.OptimisticLockConflictException;
import com.lxpantos.auth.application.port.in.AccessMemberPage;
import com.lxpantos.auth.application.port.in.AccessMemberSearchQuery;
import com.lxpantos.auth.application.port.in.AccessMemberSummary;
import com.lxpantos.auth.application.port.in.ChangeMemberAccessCommand;
import com.lxpantos.auth.application.port.out.AccessMemberQueryRepository;
import com.lxpantos.auth.application.port.out.AccessMemberRepository;
import com.lxpantos.auth.application.port.out.AccessMemberView;
import com.lxpantos.auth.application.port.out.AuditLogEntry;
import com.lxpantos.auth.application.port.out.AuditLogView;
import com.lxpantos.auth.application.port.out.CurrentMemberAccessView;
import com.lxpantos.auth.application.port.out.TransactionRunner;
import com.lxpantos.auth.domain.member.Member;
import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessManagementServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-24T01:02:03Z"), SEOUL);
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 8, 24, 10, 2, 3);

    private final FakeAccessMemberRepository writeRepo = new FakeAccessMemberRepository();
    private final FakeAccessMemberQueryRepository queryRepo = new FakeAccessMemberQueryRepository();
    private final AccessManagementService service =
            new AccessManagementService(writeRepo, queryRepo, new DirectTransactionRunner(), FIXED_CLOCK);

    private ChangeMemberAccessCommand command(Long actor, Long target, MemberRole role, MemberStatus status,
                                              String reason, long version) {
        return new ChangeMemberAccessCommand(actor, target, role, status, reason, version, "10.0.0.1");
    }

    @Test
    void changesRoleAndStatusAndWritesAuditLogInSameTransaction() {
        writeRepo.members.put(5L, member(5L, MemberRole.VIEWER, MemberStatus.ACTIVE, 0L));

        AccessMemberSummary result = service.changeAccess(
                command(1L, 5L, MemberRole.OPERATOR, MemberStatus.ACTIVE, "운영자 승격", 0L));

        assertThat(result.role()).isEqualTo(MemberRole.OPERATOR);
        assertThat(result.version()).isEqualTo(1L);
        assertThat(result.roleUpdatedAt()).isEqualTo(FIXED_NOW);
        assertThat(writeRepo.auditLogs).hasSize(1);
        AuditLogEntry log = writeRepo.auditLogs.get(0);
        assertThat(log.beforeRole()).isEqualTo(MemberRole.VIEWER);
        assertThat(log.afterRole()).isEqualTo(MemberRole.OPERATOR);
        assertThat(log.reason()).isEqualTo("운영자 승격");
        assertThat(writeRepo.transactionsCommitted).isEqualTo(1);
    }

    @Test
    void rejectsMissingReason() {
        writeRepo.members.put(5L, member(5L, MemberRole.VIEWER, MemberStatus.ACTIVE, 0L));

        assertThatThrownBy(() -> service.changeAccess(
                command(1L, 5L, MemberRole.OPERATOR, MemberStatus.ACTIVE, "  ", 0L)))
                .isInstanceOf(AccessRuleViolationException.class);
        assertThat(writeRepo.auditLogs).isEmpty();
    }

    @Test
    void rejectsTooShortReason() {
        writeRepo.members.put(5L, member(5L, MemberRole.VIEWER, MemberStatus.ACTIVE, 0L));

        assertThatThrownBy(() -> service.changeAccess(
                command(1L, 5L, MemberRole.OPERATOR, MemberStatus.ACTIVE, "abc", 0L)))
                .isInstanceOf(AccessRuleViolationException.class);
    }

    @Test
    void rejectsSelfRoleDowngrade() {
        writeRepo.members.put(1L, member(1L, MemberRole.ADMIN, MemberStatus.ACTIVE, 0L));
        queryRepo.activeAdminCount = 3;

        assertThatThrownBy(() -> service.changeAccess(
                command(1L, 1L, MemberRole.VIEWER, MemberStatus.ACTIVE, "본인 강등 시도", 0L)))
                .isInstanceOf(AccessRuleViolationException.class)
                .hasMessageContaining("본인");
    }

    @Test
    void rejectsSelfSuspension() {
        writeRepo.members.put(1L, member(1L, MemberRole.ADMIN, MemberStatus.ACTIVE, 0L));
        queryRepo.activeAdminCount = 3;

        assertThatThrownBy(() -> service.changeAccess(
                command(1L, 1L, MemberRole.ADMIN, MemberStatus.SUSPENDED, "본인 정지 시도", 0L)))
                .isInstanceOf(AccessRuleViolationException.class);
    }

    @Test
    void rejectsRemovingLastActiveAdmin() {
        writeRepo.members.put(9L, member(9L, MemberRole.ADMIN, MemberStatus.ACTIVE, 0L));
        queryRepo.activeAdminCount = 1;

        assertThatThrownBy(() -> service.changeAccess(
                command(1L, 9L, MemberRole.VIEWER, MemberStatus.ACTIVE, "마지막 관리자 강등", 0L)))
                .isInstanceOf(AccessRuleViolationException.class)
                .hasMessageContaining("관리자");
        assertThat(writeRepo.auditLogs).isEmpty();
    }

    @Test
    void allowsDemotingAdminWhenAnotherActiveAdminRemains() {
        writeRepo.members.put(9L, member(9L, MemberRole.ADMIN, MemberStatus.ACTIVE, 0L));
        writeRepo.members.put(10L, member(10L, MemberRole.ADMIN, MemberStatus.ACTIVE, 0L));
        queryRepo.activeAdminCount = 2;

        AccessMemberSummary result = service.changeAccess(
                command(1L, 9L, MemberRole.VIEWER, MemberStatus.ACTIVE, "권한 회수", 0L));

        assertThat(result.role()).isEqualTo(MemberRole.VIEWER);
    }

    @Test
    void guardBlocksConcurrentLastAdminRemovalEvenWhenPreCheckPasses() {
        // 사전 count 는 2로 통과하지만(경쟁 상태 재현), 실제 저장소에는 활성 관리자가 대상 1명뿐이라
        // 가드 업데이트가 0행을 반환해 규칙 위반(400)으로 차단된다.
        writeRepo.members.put(9L, member(9L, MemberRole.ADMIN, MemberStatus.ACTIVE, 0L));
        queryRepo.activeAdminCount = 2;

        assertThatThrownBy(() -> service.changeAccess(
                command(1L, 9L, MemberRole.VIEWER, MemberStatus.ACTIVE, "동시 강등 경쟁", 0L)))
                .isInstanceOf(AccessRuleViolationException.class)
                .hasMessageContaining("관리자");
        assertThat(writeRepo.auditLogs).isEmpty();
    }

    @Test
    void raisesConflictWhenVersionDiffers() {
        writeRepo.members.put(5L, member(5L, MemberRole.VIEWER, MemberStatus.ACTIVE, 3L));

        assertThatThrownBy(() -> service.changeAccess(
                command(1L, 5L, MemberRole.OPERATOR, MemberStatus.ACTIVE, "동시 수정", 0L)))
                .isInstanceOf(OptimisticLockConflictException.class);
        assertThat(writeRepo.auditLogs).isEmpty();
    }

    @Test
    void raisesNotFoundWhenMemberMissing() {
        assertThatThrownBy(() -> service.changeAccess(
                command(1L, 404L, MemberRole.OPERATOR, MemberStatus.ACTIVE, "없는 회원", 0L)))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void searchAppliesPagingAndMasksPhoneNumber() {
        queryRepo.total = 42;
        queryRepo.views = List.of(
                new AccessMemberView(1L, "홍길동", "hong@example.com", "01012345678",
                        MemberRole.ADMIN, MemberStatus.ACTIVE, 0L, FIXED_NOW, FIXED_NOW, null)
        );

        AccessMemberPage page = service.searchMembers(
                new AccessMemberSearchQuery("hong", MemberRole.ADMIN, MemberStatus.ACTIVE, 1, 20));

        assertThat(page.totalElements()).isEqualTo(42);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.content().get(0).maskedPhoneNumber()).isEqualTo("010-****-5678");
        assertThat(queryRepo.lastLimit).isEqualTo(20);
        assertThat(queryRepo.lastOffset).isEqualTo(20);
        assertThat(queryRepo.lastKeyword).isEqualTo("hong");
    }

    @Test
    void searchClampsSizeToMaximum() {
        queryRepo.total = 0;
        queryRepo.views = List.of();

        service.searchMembers(new AccessMemberSearchQuery(null, null, null, 0, 5000));

        assertThat(queryRepo.lastLimit).isEqualTo(100);
    }

    @Test
    void auditLogsMaskRequestIpAndReturnNewestFirst() {
        queryRepo.auditTotal = 2;
        queryRepo.auditViews = List.of(
                new AuditLogView(2L, 5L, 1L, "관리자", MemberRole.VIEWER, MemberStatus.ACTIVE,
                        MemberRole.OPERATOR, MemberStatus.ACTIVE, "두번째", "192.168.0.10", FIXED_NOW.plusMinutes(1)),
                new AuditLogView(1L, 5L, 1L, "관리자", MemberRole.VIEWER, MemberStatus.ACTIVE,
                        MemberRole.VIEWER, MemberStatus.SUSPENDED, "첫번째", "192.168.0.10", FIXED_NOW)
        );

        var page = service.auditLogs(5L, 0, 10);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).id()).isEqualTo(2L);
        assertThat(page.content().get(0).maskedRequestIp()).isEqualTo("192.168.0.***");
    }

    private Member member(Long id, MemberRole role, MemberStatus status, long version) {
        return new Member(id, "member" + id + "@example.com", "hash", "회원" + id,
                "01012345678", FIXED_NOW.minusDays(1), null, role, status, version, null);
    }

    private static final class DirectTransactionRunner implements TransactionRunner {
        @Override
        public <T> T execute(Supplier<T> action) {
            return action.get();
        }
    }

    private final class FakeAccessMemberRepository implements AccessMemberRepository {
        private final java.util.Map<Long, Member> members = new java.util.HashMap<>();
        private final List<AuditLogEntry> auditLogs = new ArrayList<>();
        private int transactionsCommitted = 0;

        @Override
        public Optional<Member> findById(Long memberId) {
            return Optional.ofNullable(members.get(memberId));
        }

        @Override
        public Optional<Member> findByEmail(String email) {
            return members.values().stream().filter(m -> m.email().equals(email)).findFirst();
        }

        @Override
        public int updateAccess(Long memberId, MemberRole role, MemberStatus status,
                                LocalDateTime roleUpdatedAt, long expectedVersion) {
            return applyUpdate(memberId, role, status, roleUpdatedAt, expectedVersion);
        }

        @Override
        public int updateAccessKeepingLastAdmin(Long memberId, MemberRole role, MemberStatus status,
                                                LocalDateTime roleUpdatedAt, long expectedVersion) {
            long otherActiveAdmins = members.values().stream()
                    .filter(m -> !m.id().equals(memberId))
                    .filter(m -> m.role() == MemberRole.ADMIN && m.status() == MemberStatus.ACTIVE)
                    .count();
            if (otherActiveAdmins == 0) {
                return 0;
            }
            return applyUpdate(memberId, role, status, roleUpdatedAt, expectedVersion);
        }

        private int applyUpdate(Long memberId, MemberRole role, MemberStatus status,
                                LocalDateTime roleUpdatedAt, long expectedVersion) {
            Member current = members.get(memberId);
            if (current == null || current.version() != expectedVersion) {
                return 0;
            }
            members.put(memberId, new Member(current.id(), current.email(), current.passwordHash(),
                    current.name(), current.phoneNumber(), current.createdAt(), current.lastLoginAt(),
                    role, status, current.version() + 1, roleUpdatedAt));
            return 1;
        }

        @Override
        public void saveAuditLog(AuditLogEntry entry) {
            auditLogs.add(entry);
            transactionsCommitted++;
        }
    }

    private static final class FakeAccessMemberQueryRepository implements AccessMemberQueryRepository {
        private long total;
        private long activeAdminCount;
        private List<AccessMemberView> views = List.of();
        private long auditTotal;
        private List<AuditLogView> auditViews = List.of();
        private String lastKeyword;
        private int lastLimit;
        private int lastOffset;

        @Override
        public long count(String keyword, MemberRole role, MemberStatus status) {
            if (role == MemberRole.ADMIN && status == MemberStatus.ACTIVE && keyword == null) {
                return activeAdminCount;
            }
            return total;
        }

        @Override
        public Optional<CurrentMemberAccessView> findAccessById(Long memberId) {
            return Optional.empty();
        }

        @Override
        public List<AccessMemberView> search(String keyword, MemberRole role, MemberStatus status,
                                             int limit, int offset) {
            this.lastKeyword = keyword;
            this.lastLimit = limit;
            this.lastOffset = offset;
            return views;
        }

        @Override
        public long countAuditLogs(Long targetMemberId) {
            return auditTotal;
        }

        @Override
        public List<AuditLogView> searchAuditLogs(Long targetMemberId, int limit, int offset) {
            return auditViews;
        }
    }
}
