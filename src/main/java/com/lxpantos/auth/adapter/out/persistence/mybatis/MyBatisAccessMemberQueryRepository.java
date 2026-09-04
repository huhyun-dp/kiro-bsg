package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.AccessMemberQueryRepository;
import com.lxpantos.auth.application.port.out.AccessMemberView;
import com.lxpantos.auth.application.port.out.AuditLogView;
import com.lxpantos.auth.application.port.out.CurrentMemberAccessView;
import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MyBatisAccessMemberQueryRepository implements AccessMemberQueryRepository {

    private static final String LIKE_ESCAPE = "!";

    private final AccessMemberQueryMapper accessMemberQueryMapper;

    public MyBatisAccessMemberQueryRepository(AccessMemberQueryMapper accessMemberQueryMapper) {
        this.accessMemberQueryMapper = accessMemberQueryMapper;
    }

    @Override
    public long count(String keyword, MemberRole role, MemberStatus status) {
        return accessMemberQueryMapper.count(toLikePattern(keyword), name(role), name(status));
    }

    @Override
    public Optional<CurrentMemberAccessView> findAccessById(Long memberId) {
        return Optional.ofNullable(accessMemberQueryMapper.findAccessById(memberId));
    }

    @Override
    public List<AccessMemberView> search(String keyword, MemberRole role, MemberStatus status, int limit, int offset) {
        return accessMemberQueryMapper.search(toLikePattern(keyword), name(role), name(status), limit, offset);
    }

    @Override
    public long countAuditLogs(Long targetMemberId) {
        return accessMemberQueryMapper.countAuditLogs(targetMemberId);
    }

    @Override
    public List<AuditLogView> searchAuditLogs(Long targetMemberId, int limit, int offset) {
        return accessMemberQueryMapper.searchAuditLogs(targetMemberId, limit, offset);
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String toLikePattern(String keyword) {
        if (keyword == null) {
            return null;
        }
        String escaped = keyword
                .replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
