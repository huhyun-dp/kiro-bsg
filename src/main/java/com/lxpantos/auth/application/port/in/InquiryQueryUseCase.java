package com.lxpantos.auth.application.port.in;

import java.util.Optional;

public interface InquiryQueryUseCase {

    InquiryPage getPage(int page, int pageSize);

    InquiryDetail getDetail(Long id);

    /** 조회수를 올리지 않고 상세를 조회한다(수정 폼 등에서 사용). */
    InquiryDetail getDetailWithoutView(Long id);

    /** 문의 작성자 회원 ID 조회(권한 판단용). 없거나 삭제된 문의면 비어 있음. */
    Optional<Long> findOwnerId(Long id);
}
