package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface InquiryMapper {

    void insert(InquiryPersistenceModel model);

    void incrementViewCount(@Param("id") Long id);

    Optional<InquiryQueryResult> findById(@Param("id") Long id);
}
