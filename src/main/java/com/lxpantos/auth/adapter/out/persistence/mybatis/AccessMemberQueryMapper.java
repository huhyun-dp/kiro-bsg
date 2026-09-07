package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.AccessMemberView;
import com.lxpantos.auth.application.port.out.AuditLogView;
import com.lxpantos.auth.application.port.out.CurrentMemberAccessView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccessMemberQueryMapper {

    long count(
            @Param("keywordPattern") String keywordPattern,
            @Param("role") String role,
            @Param("status") String status
    );

    List<AccessMemberView> search(
            @Param("keywordPattern") String keywordPattern,
            @Param("role") String role,
            @Param("status") String status,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    CurrentMemberAccessView findAccessById(@Param("memberId") Long memberId);

    long countAuditLogs(@Param("targetMemberId") Long targetMemberId);

    List<AuditLogView> searchAuditLogs(
            @Param("targetMemberId") Long targetMemberId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}
