package com.lxpantos.auth.application.port.in;

public record DeleteInquiryCommand(
        Long inquiryId,
        Long actorMemberId,
        boolean actorAdmin
) {
}
