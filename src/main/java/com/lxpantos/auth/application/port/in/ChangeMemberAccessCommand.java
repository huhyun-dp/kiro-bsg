package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

/**
 * 회원 한 명의 역할/상태 변경 요청.
 *
 * @param actorMemberId  변경을 수행하는 로그인한 관리자 ID
 * @param targetMemberId 변경 대상 회원 ID
 * @param role           변경할 역할
 * @param status         변경할 상태
 * @param reason         변경 사유
 * @param expectedVersion 낙관적 잠금 검증용, 클라이언트가 조회한 시점의 version
 * @param requestIp      요청 IP (감사 로그용)
 */
public record ChangeMemberAccessCommand(
        Long actorMemberId,
        Long targetMemberId,
        MemberRole role,
        MemberStatus status,
        String reason,
        long expectedVersion,
        String requestIp
) {
}
