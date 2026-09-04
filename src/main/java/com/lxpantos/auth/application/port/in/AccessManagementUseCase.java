package com.lxpantos.auth.application.port.in;

/**
 * 권한 관리 인바운드 포트. ADMIN 만 사용할 수 있으며, 접근 제어는 어댑터 계층에서 강제한다.
 */
public interface AccessManagementUseCase {

    AccessMemberPage searchMembers(AccessMemberSearchQuery query);

    /**
     * 회원의 역할/상태를 변경하고 감사 로그를 동일 트랜잭션으로 기록한다.
     *
     * @return 변경 후 갱신된 회원 요약
     */
    AccessMemberSummary changeAccess(ChangeMemberAccessCommand command);

    AccessAuditLogPage auditLogs(Long targetMemberId, int page, int size);
}
