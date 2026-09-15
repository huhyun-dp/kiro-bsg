package com.lxpantos.auth.application.port.in;

/**
 * Inbound port for immediate per-attachment deletion from the inquiry edit screen.
 *
 * <p>Implementations must verify the actor is the inquiry author and that the attachment
 * belongs to the inquiry, delete the metadata within the DB transaction boundary, and remove
 * the private stored file only after commit. Deleting the last attachment (leaving zero
 * attachments) is allowed.
 */
public interface DeleteInquiryAttachmentUseCase {

    void deleteAttachment(DeleteInquiryAttachmentCommand command);
}
