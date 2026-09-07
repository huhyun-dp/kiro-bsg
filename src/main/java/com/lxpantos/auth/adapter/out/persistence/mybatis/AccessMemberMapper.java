package com.lxpantos.auth.adapter.out.persistence.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AccessMemberMapper {

    MemberPersistenceModel findById(@Param("memberId") Long memberId);

    MemberPersistenceModel findByEmail(@Param("email") String email);

    int updateAccess(
            @Param("memberId") Long memberId,
            @Param("role") String role,
            @Param("status") String status,
            @Param("roleUpdatedAt") LocalDateTime roleUpdatedAt,
            @Param("expectedVersion") long expectedVersion
    );

    /**
     * 활성 관리자 강등/정지 시 사용하는 가드 업데이트.
     * version 이 일치하고, 대상 회원을 제외한 다른 활성 관리자가 1명 이상 존재할 때만 갱신한다.
     * 동시 강등으로 활성 관리자가 0명이 되는 경쟁 상태(TOCTOU)를 단일 SQL 로 차단한다.
     */
    int updateAccessKeepingLastAdmin(
            @Param("memberId") Long memberId,
            @Param("role") String role,
            @Param("status") String status,
            @Param("roleUpdatedAt") LocalDateTime roleUpdatedAt,
            @Param("expectedVersion") long expectedVersion
    );

    int insertAuditLog(AuditLogPersistenceModel entry);
}

