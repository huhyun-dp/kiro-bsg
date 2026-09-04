package com.lxpantos.auth.application.port.in;

import java.util.Optional;

/**
 * 세션 검증용 회원 접근 정보 조회 인바운드 포트.
 * 로그인 이후 상태가 바뀐 회원(정지 등)을 다음 요청에서 차단하기 위해 사용한다.
 */
public interface MemberAccessLookupUseCase {
    Optional<CurrentMemberAccess> findAccessById(Long memberId);
}
