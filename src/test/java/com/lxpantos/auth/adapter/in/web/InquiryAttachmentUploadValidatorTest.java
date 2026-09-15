package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.application.exception.InvalidInquiryAttachmentException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InquiryAttachmentUploadValidatorTest {
    private final InquiryAttachmentUploadValidator validator = new InquiryAttachmentUploadValidator();

    @Test
    void acceptsMatchingPdfSignature() {
        var result = validator.validate(List.of(file("proof.pdf", "application/pdf", "%PDF-1.7".getBytes())));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).originalFilename()).isEqualTo("proof.pdf");
    }

    @Test
    void acceptsFiveAttachmentsAndRejectsSix() {
        MockMultipartFile pdf = pdf();

        assertThat(validator.validate(List.of(pdf, pdf, pdf, pdf, pdf))).hasSize(5);
        assertThatThrownBy(() -> validator.validate(List.of(pdf, pdf, pdf, pdf, pdf, pdf)))
                .isInstanceOf(InvalidInquiryAttachmentException.class)
                .hasMessage("첨부파일은 최대 5개까지 등록할 수 있습니다.");
    }

    @Test
    void validatesFinalCountAndSizeIncludingRetainedAttachments() {
        MockMultipartFile pdf = pdf();
        long newUploadSize = pdf.getSize() * 4;

        assertThatCode(() -> validator.validate(List.of(pdf, pdf, pdf, pdf), 1,
                InquiryAttachmentUploadValidator.MAX_TOTAL_SIZE - newUploadSize)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(List.of(pdf, pdf, pdf, pdf, pdf), 1, 0))
                .isInstanceOf(InvalidInquiryAttachmentException.class)
                .hasMessage("첨부파일은 최대 5개까지 등록할 수 있습니다.");
        assertThatThrownBy(() -> validator.validate(List.of(pdf, pdf, pdf, pdf), 1,
                InquiryAttachmentUploadValidator.MAX_TOTAL_SIZE - newUploadSize + 1))
                .isInstanceOf(InvalidInquiryAttachmentException.class)
                .hasMessage("첨부파일 전체 크기는 20 MiB를 초과할 수 없습니다.");
    }

    @Test
    void rejectsPerFileSizeAndUnsafeFilenameBoundaries() {
        assertThatThrownBy(() -> validator.validate(List.of(file("large.pdf", "application/pdf",
                new byte[(int) InquiryAttachmentUploadValidator.MAX_FILE_SIZE + 1]))))
                .isInstanceOf(InvalidInquiryAttachmentException.class);
        assertThatThrownBy(() -> validator.validate(List.of(file("../proof.pdf", "application/pdf", "%PDF-".getBytes()))))
                .isInstanceOf(InvalidInquiryAttachmentException.class);
    }

    @Test
    void rejectsExtensionMimeAndSignatureMismatch() {
        assertThatThrownBy(() -> validator.validate(List.of(file("proof.pdf", "image/png", "%PDF-".getBytes()))))
                .isInstanceOf(InvalidInquiryAttachmentException.class);
        assertThatThrownBy(() -> validator.validate(List.of(file("proof.png", "image/png", "%PDF-".getBytes()))))
                .isInstanceOf(InvalidInquiryAttachmentException.class);
        assertThatThrownBy(() -> validator.validate(List.of(file("empty.pdf", "application/pdf", new byte[0]))))
                .isInstanceOf(InvalidInquiryAttachmentException.class);
    }

    private MockMultipartFile pdf() {
        return file("proof.pdf", "application/pdf", "%PDF-1.7".getBytes());
    }

    private MockMultipartFile file(String name, String type, byte[] bytes) {
        return new MockMultipartFile("attachments", name, type, bytes);
    }
}
