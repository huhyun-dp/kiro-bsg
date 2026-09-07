package com.lxpantos.auth.application.port.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 서버 사이드 페이지네이션 결과. page 는 0-base, totalElements 는 필터가 적용된 전체 건수다.
 */
public record AccessMemberPage(
        List<AccessMemberSummary> content,
        int page,
        int size,
        long totalElements
) {
    @JsonProperty("totalPages")
    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }
}
