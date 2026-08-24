package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.member.Member;

import java.util.Optional;

public interface MemberRepository {
    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    Member save(Member member);
}

