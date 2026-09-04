package com.lxpantos.auth.application.port.in;

public interface InquiryQueryUseCase {

    InquiryPage getPage(int page, int pageSize);

    InquiryDetail getDetail(Long id);
}
