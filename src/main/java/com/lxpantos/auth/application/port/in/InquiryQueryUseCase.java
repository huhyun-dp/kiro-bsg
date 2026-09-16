package com.lxpantos.auth.application.port.in;

import java.util.Optional;

public interface InquiryQueryUseCase {

    InquiryPage getPage(int page, int pageSize);

    /**
     * 조회자를 알 수 없는 상세 조회. 조회수를 1 증가시킨다.
     * 하위호환을 위해 유지하며, 조회자 회원 ID가 있으면 {@link #getDetail(Long, Long)}를 사용한다.
     */
    default InquiryDetail getDetail(Long id) {
        return getDetail(id, null);
    }

    /**
     * 상세를 조회한다. 조회자가 작성자 본인이면 조회수를 증가시키지 않고(셀프 카운트 방지),
     * 그 외(다른 회원이거나 조회자 미상)에는 조회수를 정확히 1 증가시킨다.
     *
     * @param id             문의 ID
     * @param viewerMemberId 조회하는 회원 ID. {@code null}이면 조회수를 증가시킨다.
     */
    InquiryDetail getDetail(Long id, Long viewerMemberId);

    /** 조회수를 올리지 않고 상세를 조회한다(수정 폼 등에서 사용). */
    InquiryDetail getDetailWithoutView(Long id);

    /** 문의 작성자 회원 ID 조회(권한 판단용). 없거나 삭제된 문의면 비어 있음. */
    Optional<Long> findOwnerId(Long id);
}
