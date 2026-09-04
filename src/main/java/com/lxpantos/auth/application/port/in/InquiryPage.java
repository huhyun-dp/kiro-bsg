package com.lxpantos.auth.application.port.in;

import java.util.List;

public record InquiryPage(
        List<InquirySummary> items,
        int currentPage,
        int totalPages,
        long totalCount
) {
    public boolean hasPrevious() {
        return currentPage > 1;
    }

    public boolean hasNext() {
        return currentPage < totalPages;
    }
}
