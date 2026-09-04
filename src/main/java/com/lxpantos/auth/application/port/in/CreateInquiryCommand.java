package com.lxpantos.auth.application.port.in;

public record CreateInquiryCommand(
        Long memberId,
        String title,
        String content
) {
}
