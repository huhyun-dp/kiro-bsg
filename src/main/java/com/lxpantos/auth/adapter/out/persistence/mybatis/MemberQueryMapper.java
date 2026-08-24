package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.in.MemberSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberQueryMapper {
    List<MemberSummary> search(@Param("keywordPattern") String keywordPattern);
}
