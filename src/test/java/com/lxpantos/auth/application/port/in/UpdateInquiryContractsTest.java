package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.application.port.out.InquiryAttachmentAggregate;
import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateInquiryContractsTest {

    @Test
    void normalizesAbsentAttachmentChangesToImmutableEmptyCollections() {
        UpdateInquiryCommand command = new UpdateInquiryCommand(10L, 2L, false, "title", "content", null, null);

        assertThat(command.newAttachments()).isEmpty();
        assertThat(command.attachmentIdsToDelete()).isEmpty();
        assertThatThrownBy(() -> command.attachmentIdsToDelete().add(1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resultRetainsOnlyImmutableDeletedAttachmentMetadata() {
        InquiryAttachment deleted = new InquiryAttachment(1L, 10L, "00000000-0000-0000-0000-000000000001",
                "proof.pdf", AttachmentMediaType.PDF, 8L, LocalDateTime.of(2024, 1, 1, 0, 0));

        UpdateInquiryResult result = new UpdateInquiryResult(10L, List.of(deleted));

        assertThat(result.inquiryId()).isEqualTo(10L);
        assertThat(result.deletedAttachments()).containsExactly(deleted);
        assertThatThrownBy(() -> result.deletedAttachments().add(deleted))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aggregateExposesRetainedAttachmentCountAndTotalSize() {
        InquiryAttachmentAggregate aggregate = new InquiryAttachmentAggregate(5L, 20L);

        assertThat(aggregate.count()).isEqualTo(5L);
        assertThat(aggregate.totalFileSize()).isEqualTo(20L);
    }
}
