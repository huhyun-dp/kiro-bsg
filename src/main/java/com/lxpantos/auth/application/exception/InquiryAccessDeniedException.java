package com.lxpantos.auth.application.exception;

public class InquiryAccessDeniedException extends RuntimeException {

    public InquiryAccessDeniedException() {
        super("해당 문의를 수정하거나 삭제할 권한이 없습니다.");
    }
}
