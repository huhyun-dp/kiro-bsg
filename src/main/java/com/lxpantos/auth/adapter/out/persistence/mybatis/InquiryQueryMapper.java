package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.InquiryListQueryResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InquiryQueryMapper {

    List<InquiryListQueryResult> findPage(@Param("offset") int offset, @Param("limit") int limit);

    long countAll();
}
