package com.lxpantos.auth.application.exception;

public class InquiryNotFoundException extends RuntimeException {

    public InquiryNotFoundException(Long id) {
        super("문의를 찾을 수 없습니다. id=" + id);
    }
}
