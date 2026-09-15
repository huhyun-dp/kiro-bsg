package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.application.exception.InvalidInquiryAttachmentException;
import com.lxpantos.auth.application.port.in.PendingInquiryAttachment;
import com.lxpantos.auth.domain.inquiry.AttachmentMediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class InquiryAttachmentUploadValidator {
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    public static final long MAX_TOTAL_SIZE = 20L * 1024 * 1024;
    public static final int MAX_ATTACHMENT_COUNT = 5;

    public List<PendingInquiryAttachment> validate(List<MultipartFile> sourceFiles) {
        return validate(sourceFiles, 0, 0);
    }

    /**
     * Validates newly uploaded files against the attachments retained by an edit.
     * The retained values must represent attachments that remain after requested deletions.
     */
    public List<PendingInquiryAttachment> validate(List<MultipartFile> sourceFiles, int retainedAttachmentCount,
                                                   long retainedAttachmentSize) {
        if (retainedAttachmentCount < 0 || retainedAttachmentSize < 0) {
            throw new IllegalArgumentException("Retained attachment count and size must not be negative.");
        }

        List<MultipartFile> candidates = sourceFiles == null ? List.of() : sourceFiles;
        List<MultipartFile> files = candidates.stream()
                .filter(file -> file != null && !(file.isEmpty() && (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())))
                .toList();
        if (files.stream().anyMatch(MultipartFile::isEmpty)) {
            throw new InvalidInquiryAttachmentException("빈 파일은 첨부할 수 없습니다.");
        }
        if (retainedAttachmentCount + files.size() > MAX_ATTACHMENT_COUNT) {
            throw new InvalidInquiryAttachmentException("첨부파일은 최대 5개까지 등록할 수 있습니다.");
        }

        long newUploadSize = 0;
        List<PendingInquiryAttachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            long size = file.getSize();
            if (size <= 0) throw new InvalidInquiryAttachmentException("빈 파일은 첨부할 수 없습니다.");
            if (size > MAX_FILE_SIZE) throw new InvalidInquiryAttachmentException("파일 하나는 10 MiB를 초과할 수 없습니다.");
            newUploadSize = addSize(newUploadSize, size);
            if (newUploadSize > MAX_TOTAL_SIZE) {
                throw new InvalidInquiryAttachmentException("신규 첨부파일 전체 크기는 20 MiB를 초과할 수 없습니다.");
            }
            String filename = safeFilename(file.getOriginalFilename());
            AttachmentMediaType mediaType = mediaType(filename, file.getContentType());
            verifyMagicBytes(file, mediaType);
            attachments.add(new PendingInquiryAttachment(UUID.randomUUID().toString(), filename, mediaType, size,
                    file::getInputStream));
        }
        if (addSize(retainedAttachmentSize, newUploadSize) > MAX_TOTAL_SIZE) {
            throw new InvalidInquiryAttachmentException("첨부파일 전체 크기는 20 MiB를 초과할 수 없습니다.");
        }
        return List.copyOf(attachments);
    }

    private long addSize(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException e) {
            throw new InvalidInquiryAttachmentException("첨부파일 전체 크기는 20 MiB를 초과할 수 없습니다.");
        }
    }

    private String safeFilename(String original) {
        if (original == null || original.isBlank()) throw new InvalidInquiryAttachmentException("파일 이름이 올바르지 않습니다.");
        String filename = Normalizer.normalize(original, Normalizer.Form.NFC);
        if (filename.codePointCount(0, filename.length()) > 255 || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0
                || filename.chars().anyMatch(value -> value <= 0x1F || value == 0x7F)) {
            throw new InvalidInquiryAttachmentException("파일 이름이 올바르지 않습니다.");
        }
        return filename;
    }

    private AttachmentMediaType mediaType(String filename, String declaredContentType) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) throw new InvalidInquiryAttachmentException("허용되지 않는 파일 형식입니다.");
        String extension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (AttachmentMediaType value : AttachmentMediaType.values()) {
            if (value.supportsExtension(extension) && value.matchesDeclaredContentType(declaredContentType)) return value;
        }
        throw new InvalidInquiryAttachmentException("PDF, PNG, JPEG 파일만 첨부할 수 있습니다.");
    }

    private void verifyMagicBytes(MultipartFile file, AttachmentMediaType mediaType) {
        try (InputStream input = file.getInputStream()) {
            byte[] prefix = input.readNBytes(8);
            boolean matches = switch (mediaType) {
                case PDF -> prefix.length >= 5 && prefix[0] == '%' && prefix[1] == 'P' && prefix[2] == 'D' && prefix[3] == 'F' && prefix[4] == '-';
                case PNG -> prefix.length == 8 && prefix[0] == (byte) 0x89 && prefix[1] == 0x50 && prefix[2] == 0x4E && prefix[3] == 0x47
                        && prefix[4] == 0x0D && prefix[5] == 0x0A && prefix[6] == 0x1A && prefix[7] == 0x0A;
                case JPEG -> prefix.length >= 3 && prefix[0] == (byte) 0xFF && prefix[1] == (byte) 0xD8 && prefix[2] == (byte) 0xFF;
            };
            if (!matches) throw new InvalidInquiryAttachmentException("파일 내용을 확인할 수 없습니다.");
        } catch (IOException e) {
            throw new InvalidInquiryAttachmentException("파일 내용을 확인할 수 없습니다.");
        }
    }
}
