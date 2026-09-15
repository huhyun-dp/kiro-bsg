package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.domain.inquiry.Inquiry;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MyBatisInquiryRepository implements InquiryRepository {

    private final InquiryMapper inquiryMapper;

    public MyBatisInquiryRepository(InquiryMapper inquiryMapper) {
        this.inquiryMapper = inquiryMapper;
    }

    @Override
    public Long save(Inquiry inquiry) {
        InquiryPersistenceModel model = new InquiryPersistenceModel(
                inquiry.id(),
                inquiry.memberId(),
                inquiry.title(),
                inquiry.content(),
                inquiry.viewCount(),
                inquiry.createdAt()
        );
        inquiryMapper.insert(model);
        return model.getId();
    }

    @Override
    public void incrementViewCount(Long id) {
        inquiryMapper.incrementViewCount(id);
    }

    @Override
    public Optional<InquiryQueryResult> findById(Long id) {
        return inquiryMapper.findById(id);
    }

    @Override
    public Optional<Long> findOwnerId(Long id) {
        return inquiryMapper.findOwnerId(id);
    }

    @Override
    public Optional<Long> findOwnerIdForUpdate(Long id) {
        return inquiryMapper.findOwnerIdForUpdate(id);
    }

    @Override
    public int updateContent(Long id, String title, String content) {
        return inquiryMapper.updateContent(id, title, content);
    }

    @Override
    public int softDelete(Long id) {
        return inquiryMapper.softDelete(id);
    }
}
