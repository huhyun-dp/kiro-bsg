package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.MemberQueryResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberQueryMapper {
    List<MemberQueryResult> search(@Param("keywordPattern") String keywordPattern);
}
