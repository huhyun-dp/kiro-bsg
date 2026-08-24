package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.port.in.MemberQueryUseCase;
import com.lxpantos.auth.application.port.in.MemberSummary;
import com.lxpantos.auth.application.port.out.MemberQueryRepository;

import java.util.List;
import java.util.Locale;

public class MemberQueryService implements MemberQueryUseCase {

    private final MemberQueryRepository memberQueryRepository;

    public MemberQueryService(MemberQueryRepository memberQueryRepository) {
        this.memberQueryRepository = memberQueryRepository;
    }

    @Override
    public List<MemberSummary> search(String keyword) {
        return memberQueryRepository.search(normalizeKeyword(keyword));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String stripped = keyword.strip().toLowerCase(Locale.ROOT);
        return stripped.isEmpty() ? null : stripped;
    }
}
