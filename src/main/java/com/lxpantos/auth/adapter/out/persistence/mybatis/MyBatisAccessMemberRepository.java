package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.AccessMemberRepository;
import com.lxpantos.auth.application.port.out.AuditLogEntry;
import com.lxpantos.auth.domain.member.Member;
import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MyBatisAccessMemberRepository implements AccessMemberRepository {

    private final AccessMemberMapper accessMemberMapper;

    public MyBatisAccessMemberRepository(AccessMemberMapper accessMemberMapper) {
        this.accessMemberMapper = accessMemberMapper;
    }

    @Override
    public Optional<Member> findById(Long memberId) {
        return Optional.ofNullable(accessMemberMapper.findById(memberId)).map(this::toDomain);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return Optional.ofNullable(accessMemberMapper.findByEmail(email)).map(this::toDomain);
    }

    @Override
    public int updateAccess(
            Long memberId,
            MemberRole role,
            MemberStatus status,
            LocalDateTime roleUpdatedAt,
            long expectedVersion
    ) {
        return accessMemberMapper.updateAccess(
                memberId,
                role.name(),
                status.name(),
                roleUpdatedAt,
                expectedVersion
        );
    }

    @Override
    public int updateAccessKeepingLastAdmin(
            Long memberId,
            MemberRole role,
            MemberStatus status,
            LocalDateTime roleUpdatedAt,
            long expectedVersion
    ) {
        return accessMemberMapper.updateAccessKeepingLastAdmin(
                memberId,
                role.name(),
                status.name(),
                roleUpdatedAt,
                expectedVersion
        );
    }

    @Override
    public void saveAuditLog(AuditLogEntry entry) {
        AuditLogPersistenceModel model = new AuditLogPersistenceModel();
        model.setTargetMemberId(entry.targetMemberId());
        model.setActorMemberId(entry.actorMemberId());
        model.setBeforeRole(entry.beforeRole().name());
        model.setBeforeStatus(entry.beforeStatus().name());
        model.setAfterRole(entry.afterRole().name());
        model.setAfterStatus(entry.afterStatus().name());
        model.setReason(entry.reason());
        model.setRequestIp(entry.requestIp());
        model.setCreatedAt(entry.createdAt());
        accessMemberMapper.insertAuditLog(model);
    }

    private Member toDomain(MemberPersistenceModel model) {
        return new Member(
                model.getId(),
                model.getEmail(),
                model.getPasswordHash(),
                model.getName(),
                model.getPhoneNumber(),
                model.getCreatedAt(),
                model.getLastLoginAt(),
                model.getRole() == null ? MemberRole.VIEWER : MemberRole.valueOf(model.getRole()),
                model.getStatus() == null ? MemberStatus.ACTIVE : MemberStatus.valueOf(model.getStatus()),
                model.getVersion(),
                model.getRoleUpdatedAt()
        );
    }
}
