package com.lxpantos.auth.adapter.out.persistence.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface MemberMapper {
    int countByEmail(@Param("email") String email);

    MemberPersistenceModel findByEmail(@Param("email") String email);

    int insert(MemberPersistenceModel member);

    int updateLastLoginAt(@Param("memberId") Long memberId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
