package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InquiryQueryMapper {

    List<InquiryQueryResult> findPage(@Param("offset") int offset, @Param("limit") int limit);

    long countAll();
}
