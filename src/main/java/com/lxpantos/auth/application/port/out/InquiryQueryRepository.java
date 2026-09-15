package com.lxpantos.auth.application.port.out;

import java.util.List;

public interface InquiryQueryRepository {

    List<InquiryListQueryResult> findPage(int offset, int limit);

    long countAll();
}
