package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;

/**
 * 권한 관리 목록 검색 조건. 값은 null 을 허용하며 서비스에서 정규화한다.
 */
public record AccessMemberSearchQuery(
        String keyword,
        MemberRole role,
        MemberStatus status,
        int page,
        int size
) {
}
