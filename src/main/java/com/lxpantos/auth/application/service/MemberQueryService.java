package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.port.in.MemberQueryUseCase;
import com.lxpantos.auth.application.port.in.MemberSummary;
import com.lxpantos.auth.application.port.out.MemberQueryRepository;
import com.lxpantos.auth.application.port.out.MemberQueryResult;

import java.util.List;
import java.util.Locale;

public class MemberQueryService implements MemberQueryUseCase {

    private final MemberQueryRepository memberQueryRepository;

    public MemberQueryService(MemberQueryRepository memberQueryRepository) {
        this.memberQueryRepository = memberQueryRepository;
    }

    @Override
    public List<MemberSummary> search(String keyword) {
        return memberQueryRepository.search(normalizeKeyword(keyword)).stream()
                .map(this::toSummary)
                .toList();
    }

    private MemberSummary toSummary(MemberQueryResult result) {
        return new MemberSummary(
                result.id(),
                result.name(),
                maskEmail(result.email()),
                maskPhoneNumber(result.phoneNumber()),
                result.createdAt(),
                result.lastLoginAt()
        );
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            // '@' 가 없거나 첫 글자 바로 다음에 오는 경우 — 로컬 파트가 마스킹할 길이 없음
            return email;
        }
        String masked = "*".repeat(atIndex - 1);
        return email.charAt(0) + masked + email.substring(atIndex);
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String stripped = keyword.strip().toLowerCase(Locale.ROOT);
        return stripped.isEmpty() ? null : stripped;
    }
}
