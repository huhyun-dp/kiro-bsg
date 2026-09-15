package com.lxpantos.auth.application.exception;

public class InquiryAttachmentStorageException extends RuntimeException {
    public InquiryAttachmentStorageException() { super("첨부파일을 처리하지 못했습니다."); }
    public InquiryAttachmentStorageException(Throwable cause) { super("첨부파일을 처리하지 못했습니다.", cause); }
}
