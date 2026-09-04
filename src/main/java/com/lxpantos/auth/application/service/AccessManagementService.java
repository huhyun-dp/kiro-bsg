package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.AccessRuleViolationException;
import com.lxpantos.auth.application.exception.MemberNotFoundException;
import com.lxpantos.auth.application.exception.OptimisticLockConflictException;
import com.lxpantos.auth.application.port.in.AccessAuditLogEntry;
import com.lxpantos.auth.application.port.in.AccessAuditLogPage;
import com.lxpantos.auth.application.port.in.AccessManagementUseCase;
import com.lxpantos.auth.application.port.in.AccessMemberPage;
import com.lxpantos.auth.application.port.in.AccessMemberSearchQuery;
import com.lxpantos.auth.application.port.in.AccessMemberSummary;
import com.lxpantos.auth.application.port.in.ChangeMemberAccessCommand;
import com.lxpantos.auth.application.port.in.CurrentMemberAccess;
import com.lxpantos.auth.application.port.in.MemberAccessLookupUseCase;
import com.lxpantos.auth.application.port.out.AccessMemberQueryRepository;
import com.lxpantos.auth.application.port.out.AccessMemberRepository;
import com.lxpantos.auth.application.port.out.AccessMemberView;
import com.lxpantos.auth.application.port.out.AuditLogEntry;
import com.lxpantos.auth.application.port.out.AuditLogView;
import com.lxpantos.auth.application.port.out.TransactionRunner;
import com.lxpantos.auth.domain.member.Member;
import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class AccessManagementService implements AccessManagementUseCase, MemberAccessLookupUseCase {

    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_KEYWORD_LENGTH = 100;
    static final int MIN_REASON_LENGTH = 4;
    static final int MAX_REASON_LENGTH = 500;

    private final AccessMemberRepository accessMemberRepository;
    private final AccessMemberQueryRepository accessMemberQueryRepository;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public AccessManagementService(
            AccessMemberRepository accessMemberRepository,
            AccessMemberQueryRepository accessMemberQueryRepository,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        this.accessMemberRepository = accessMemberRepository;
        this.accessMemberQueryRepository = accessMemberQueryRepository;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    @Override
    public AccessMemberPage searchMembers(AccessMemberSearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        String keyword = normalizeKeyword(query.keyword());
        int size = normalizeSize(query.size());
        int page = Math.max(query.page(), 0);
        int offset = page * size;

        long total = accessMemberQueryRepository.count(keyword, query.role(), query.status());
        List<AccessMemberSummary> content = accessMemberQueryRepository
                .search(keyword, query.role(), query.status(), size, offset).stream()
                .map(this::toSummary)
                .toList();
        return new AccessMemberPage(content, page, size, total);
    }

    @Override
    public AccessMemberSummary changeAccess(ChangeMemberAccessCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.actorMemberId(), "actorMemberId must not be null");
        Objects.requireNonNull(command.targetMemberId(), "targetMemberId must not be null");
        MemberRole newRole = Objects.requireNonNull(command.role(), "role must not be null");
        MemberStatus newStatus = Objects.requireNonNull(command.status(), "status must not be null");
        String reason = normalizeReason(command.reason());

        return transactionRunner.execute(() -> {
            Member target = accessMemberRepository.findById(command.targetMemberId())
                    .orElseThrow(MemberNotFoundException::new);

            MemberRole beforeRole = target.role();
            MemberStatus beforeStatus = target.status();

            validateSelfChange(command.actorMemberId(), target, newRole, newStatus);

            boolean removesActiveAdmin = isRemovingActiveAdmin(beforeRole, beforeStatus, newRole, newStatus);
            // 흔한 경우에 친절한 400 메시지를 주기 위한 사전 검증(경쟁 상태는 아래 가드 업데이트가 최종 차단).
            if (removesActiveAdmin) {
                validateLastActiveAdmin(target.id());
            }

            LocalDateTime now = LocalDateTime.now(clock);
            int updated = removesActiveAdmin
                    ? accessMemberRepository.updateAccessKeepingLastAdmin(
                            target.id(), newRole, newStatus, now, command.expectedVersion())
                    : accessMemberRepository.updateAccess(
                            target.id(), newRole, newStatus, now, command.expectedVersion());
            if (updated == 0) {
                // version 이 여전히 일치하면 마지막 관리자 가드가 걸린 것이고, 아니면 동시 수정 충돌이다.
                if (removesActiveAdmin && stillAtVersion(target.id(), command.expectedVersion())) {
                    throw new AccessRuleViolationException("활성 상태의 관리자가 최소 1명 이상 남아 있어야 합니다.");
                }
                throw new OptimisticLockConflictException();
            }

            accessMemberRepository.saveAuditLog(new AuditLogEntry(
                    target.id(),
                    command.actorMemberId(),
                    beforeRole,
                    beforeStatus,
                    newRole,
                    newStatus,
                    reason,
                    command.requestIp(),
                    now
            ));

            return new AccessMemberSummary(
                    target.id(),
                    target.name(),
                    target.email(),
                    maskPhoneNumber(target.phoneNumber()),
                    newRole,
                    newStatus,
                    command.expectedVersion() + 1,
                    now,
                    target.createdAt(),
                    target.lastLoginAt()
            );
        });
    }

    @Override
    public AccessAuditLogPage auditLogs(Long targetMemberId, int page, int size) {
        Objects.requireNonNull(targetMemberId, "targetMemberId must not be null");
        int normalizedSize = normalizeSize(size);
        int normalizedPage = Math.max(page, 0);
        int offset = normalizedPage * normalizedSize;

        long total = accessMemberQueryRepository.countAuditLogs(targetMemberId);
        List<AccessAuditLogEntry> content = accessMemberQueryRepository
                .searchAuditLogs(targetMemberId, normalizedSize, offset).stream()
                .map(this::toAuditEntry)
                .toList();
        return new AccessAuditLogPage(content, normalizedPage, normalizedSize, total);
    }

    @Override
    public Optional<CurrentMemberAccess> findAccessById(Long memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        return accessMemberQueryRepository.findAccessById(memberId)
                .map(view -> new CurrentMemberAccess(view.id(), view.role(), view.status()));
    }

    private void validateSelfChange(Long actorMemberId, Member target, MemberRole newRole, MemberStatus newStatus) {
        if (!Objects.equals(actorMemberId, target.id())) {
            return;
        }
        if (newRole != MemberRole.ADMIN) {
            throw new AccessRuleViolationException("본인의 관리자 역할을 낮출 수 없습니다.");
        }
        if (newStatus != MemberStatus.ACTIVE) {
            throw new AccessRuleViolationException("본인 계정을 정지할 수 없습니다.");
        }
    }

    private boolean isRemovingActiveAdmin(
            MemberRole beforeRole,
            MemberStatus beforeStatus,
            MemberRole newRole,
            MemberStatus newStatus
    ) {
        boolean wasActiveAdmin = beforeRole == MemberRole.ADMIN && beforeStatus == MemberStatus.ACTIVE;
        boolean staysActiveAdmin = newRole == MemberRole.ADMIN && newStatus == MemberStatus.ACTIVE;
        return wasActiveAdmin && !staysActiveAdmin;
    }

    private void validateLastActiveAdmin(Long targetMemberId) {
        // 대상 회원을 포함한 활성 관리자가 1명 이하이면(= 대상이 유일) 강등/정지를 막는다.
        if (accessMemberQueryRepository.count(null, MemberRole.ADMIN, MemberStatus.ACTIVE) <= 1) {
            throw new AccessRuleViolationException("활성 상태의 관리자가 최소 1명 이상 남아 있어야 합니다.");
        }
    }

    private boolean stillAtVersion(Long memberId, long expectedVersion) {
        return accessMemberRepository.findById(memberId)
                .map(m -> m.version() == expectedVersion)
                .orElse(false);
    }

    private AccessMemberSummary toSummary(AccessMemberView view) {
        return new AccessMemberSummary(
                view.id(),
                view.name(),
                view.email(),
                maskPhoneNumber(view.phoneNumber()),
                view.role(),
                view.status(),
                view.version(),
                view.roleUpdatedAt(),
                view.createdAt(),
                view.lastLoginAt()
        );
    }

    private AccessAuditLogEntry toAuditEntry(AuditLogView view) {
        return new AccessAuditLogEntry(
                view.id(),
                view.targetMemberId(),
                view.actorMemberId(),
                view.actorName(),
                view.beforeRole(),
                view.beforeStatus(),
                view.afterRole(),
                view.afterStatus(),
                view.reason(),
                maskIp(view.requestIp()),
                view.createdAt()
        );
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String stripped = keyword.strip();
        if (stripped.isEmpty()) {
            return null;
        }
        if (stripped.length() > MAX_KEYWORD_LENGTH) {
            throw new AccessRuleViolationException("검색어는 " + MAX_KEYWORD_LENGTH + "자 이하여야 합니다.");
        }
        return stripped.toLowerCase(Locale.ROOT);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.strip().isEmpty()) {
            throw new AccessRuleViolationException("변경 사유를 입력해 주세요.");
        }
        String stripped = reason.strip();
        if (stripped.length() < MIN_REASON_LENGTH) {
            throw new AccessRuleViolationException("변경 사유는 " + MIN_REASON_LENGTH + "자 이상 입력해 주세요.");
        }
        if (stripped.length() > MAX_REASON_LENGTH) {
            throw new AccessRuleViolationException("변경 사유는 " + MAX_REASON_LENGTH + "자 이하여야 합니다.");
        }
        return stripped;
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return "미등록";
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() != 11) {
            return "****";
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(7);
    }

    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "-";
        }
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".***";
        }
        int lastColon = ip.lastIndexOf(':');
        if (lastColon > 0) {
            return ip.substring(0, lastColon) + ":****";
        }
        return "***";
    }
}
