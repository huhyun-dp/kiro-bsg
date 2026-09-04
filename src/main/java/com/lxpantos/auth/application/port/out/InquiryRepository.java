package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.domain.inquiry.Inquiry;

import java.util.Optional;

public interface InquiryRepository {

    Long save(Inquiry inquiry);

    void incrementViewCount(Long id);

    Optional<InquiryQueryResult> findById(Long id);
}
