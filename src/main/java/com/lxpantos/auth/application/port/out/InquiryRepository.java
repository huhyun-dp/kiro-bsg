package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.inquiry.Inquiry;

import java.util.Optional;

public interface InquiryRepository {

    Long save(Inquiry inquiry);

    void incrementViewCount(Long id);

    Optional<InquiryQueryResult> findById(Long id);

    /** 권한 검증용 작성자 회원 ID 조회. 존재하지 않거나 삭제된 문의면 비어 있음. */
    Optional<Long> findOwnerId(Long id);

    /**
     * 편집 트랜잭션에서 문의 행을 잠그고 작성자 회원 ID를 조회한다.
     * 같은 문의의 첨부 aggregate 검증과 metadata 변경은 이 호출 이후 동일 트랜잭션에서 수행해야 한다.
     */
    default Optional<Long> findOwnerIdForUpdate(Long id) {
        return findOwnerId(id);
    }

    /** 제목·내용 수정. 삭제되지 않은 문의만 대상. 영향 행 수 반환. */
    int updateContent(Long id, String title, String content);

    /** 논리 삭제(deleted = true). 삭제되지 않은 문의만 대상. 영향 행 수 반환. */
    int softDelete(Long id);
}
