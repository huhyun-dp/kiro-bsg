package com.lxpantos.auth.application.port.in;

import java.util.List;

/**
 * 감사 로그 페이지네이션 결과. content 는 최신순으로 정렬된다.
 */
public record AccessAuditLogPage(
        List<AccessAuditLogEntry> content,
        int page,
        int size,
        long totalElements
) {
    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }
}
