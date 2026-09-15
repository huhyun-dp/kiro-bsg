package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.InquiryQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryListQueryResult;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MyBatisInquiryQueryRepository implements InquiryQueryRepository {

    private final InquiryQueryMapper inquiryQueryMapper;

    public MyBatisInquiryQueryRepository(InquiryQueryMapper inquiryQueryMapper) {
        this.inquiryQueryMapper = inquiryQueryMapper;
    }

    @Override
    public List<InquiryListQueryResult> findPage(int offset, int limit) {
        return inquiryQueryMapper.findPage(offset, limit);
    }

    @Override
    public long countAll() {
        return inquiryQueryMapper.countAll();
    }
}
