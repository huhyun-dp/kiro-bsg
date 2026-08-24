package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.exception.DuplicateEmailException;
import com.lxpantos.auth.application.port.out.MemberRepository;
import com.lxpantos.auth.domain.member.Member;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MyBatisMemberRepository implements MemberRepository {

    private final MemberMapper memberMapper;

    public MyBatisMemberRepository(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberMapper.countByEmail(email) > 0;
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return Optional.ofNullable(memberMapper.findByEmail(email)).map(this::toDomain);
    }

    @Override
    public Member save(Member member) {
        MemberPersistenceModel model = toPersistenceModel(member);
        try {
            memberMapper.insert(model);
        } catch (DuplicateKeyException exception) {
            throw new DuplicateEmailException();
        }
        return toDomain(model);
    }

    @Override
    public void updateLastLoginAt(Long memberId, LocalDateTime lastLoginAt) {
        memberMapper.updateLastLoginAt(memberId, lastLoginAt);
    }

    private Member toDomain(MemberPersistenceModel model) {
        return new Member(
                model.getId(),
                model.getEmail(),
                model.getPasswordHash(),
                model.getName(),
                model.getPhoneNumber(),
                model.getCreatedAt(),
                model.getLastLoginAt()
        );
    }

    private MemberPersistenceModel toPersistenceModel(Member member) {
        MemberPersistenceModel model = new MemberPersistenceModel();
        model.setId(member.id());
        model.setEmail(member.email());
        model.setPasswordHash(member.passwordHash());
        model.setName(member.name());
        model.setPhoneNumber(member.phoneNumber());
        model.setCreatedAt(member.createdAt());
        model.setLastLoginAt(member.lastLoginAt());
        return model;
    }
}
