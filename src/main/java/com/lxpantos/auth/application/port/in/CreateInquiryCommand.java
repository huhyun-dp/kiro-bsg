package com.lxpantos.auth.application.port.in;

import java.util.List;

public record CreateInquiryCommand(
        Long memberId,
        String title,
        String content,
        List<PendingInquiryAttachment> attachments
) {
    public CreateInquiryCommand(Long memberId, String title, String content) {
        this(memberId, title, content, List.of());
    }
}
