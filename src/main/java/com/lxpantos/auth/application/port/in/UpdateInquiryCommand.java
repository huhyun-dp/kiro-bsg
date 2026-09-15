package com.lxpantos.auth.application.port.in;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validated edit input. Web-layer multipart types, file paths, and raw streams are kept out
 * of this boundary; new files are represented only by validated pending attachment metadata.
 * The actorAdmin field is retained for source compatibility with the existing web command
 * shape, but inquiry editing is author-only and the application service must ignore it.
 */
public record UpdateInquiryCommand(
        Long inquiryId,
        Long actorMemberId,
        boolean actorAdmin,
        String title,
        String content,
        List<PendingInquiryAttachment> newAttachments,
        Set<Long> attachmentIdsToDelete
) {
    public UpdateInquiryCommand {
        newAttachments = List.copyOf(Objects.requireNonNullElse(newAttachments, List.of()));
        attachmentIdsToDelete = Set.copyOf(Objects.requireNonNullElse(attachmentIdsToDelete, Set.of()));
    }

    /**
     * Temporary source-compatible constructor for the existing title/content-only flow.
     * New edit adapters must provide attachment additions and explicit deletion IDs.
     */
    public UpdateInquiryCommand(Long inquiryId, Long actorMemberId, boolean actorAdmin, String title, String content) {
        this(inquiryId, actorMemberId, actorAdmin, title, content, List.of(), Set.of());
    }
}
