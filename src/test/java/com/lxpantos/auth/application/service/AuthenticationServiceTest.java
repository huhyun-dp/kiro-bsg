package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.port.in.AuthenticatedMember;
import com.lxpantos.auth.application.port.in.LoginCommand;
import com.lxpantos.auth.application.port.in.RegisterMemberCommand;
import com.lxpantos.auth.application.port.out.MemberRepository;
import com.lxpantos.auth.application.port.out.PasswordHasher;
import com.lxpantos.auth.domain.member.Member;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-24T01:02:03Z"), SEOUL);

    @Test
    void registersNormalizedPhoneNumberWithKoreanLocalTime() {
        InMemoryMemberRepository repository = new InMemoryMemberRepository();
        AuthenticationService service = new AuthenticationService(repository, new StubPasswordHasher(), FIXED_CLOCK);

        Long memberId = service.register(new RegisterMemberCommand(
                " USER@Example.com ",
                "password1",
                " 홍길동 ",
                "010-1234-5678"
        ));

        assertThat(memberId).isEqualTo(1L);
        assertThat(repository.member.email()).isEqualTo("user@example.com");
        assertThat(repository.member.name()).isEqualTo("홍길동");
        assertThat(repository.member.phoneNumber()).isEqualTo("01012345678");
        assertThat(repository.member.createdAt()).isEqualTo(LocalDateTime.of(2026, 8, 24, 10, 2, 3));
        assertThat(repository.member.lastLoginAt()).isNull();
    }

    @Test
    void rejectsInvalidMobilePhoneNumber() {
        AuthenticationService service = new AuthenticationService(
                new InMemoryMemberRepository(),
                new StubPasswordHasher(),
                FIXED_CLOCK
        );

        assertThatThrownBy(() -> service.register(new RegisterMemberCommand(
                "user@example.com",
                "password1",
                "홍길동",
                "02-1234-5678"
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatesLastLoginAtWithKoreanLocalTimeAfterSuccessfulLogin() {
        InMemoryMemberRepository repository = new InMemoryMemberRepository();
        repository.member = new Member(
                7L,
                "user@example.com",
                "hashed-password",
                "홍길동",
                "01012345678",
                LocalDateTime.of(2026, 8, 20, 9, 0),
                null
        );
        AuthenticationService service = new AuthenticationService(repository, new StubPasswordHasher(), FIXED_CLOCK);

        AuthenticatedMember authenticated = service.login(new LoginCommand("user@example.com", "password1"));

        assertThat(authenticated.id()).isEqualTo(7L);
        assertThat(repository.updatedMemberId).isEqualTo(7L);
        assertThat(repository.lastLoginAt).isEqualTo(LocalDateTime.of(2026, 8, 24, 10, 2, 3));
    }

    private static class InMemoryMemberRepository implements MemberRepository {
        private Member member;
        private Long updatedMemberId;
        private LocalDateTime lastLoginAt;

        @Override
        public boolean existsByEmail(String email) {
            return member != null && member.email().equals(email);
        }

        @Override
        public Optional<Member> findByEmail(String email) {
            return member != null && member.email().equals(email) ? Optional.of(member) : Optional.empty();
        }

        @Override
        public Member save(Member newMember) {
            member = new Member(
                    1L,
                    newMember.email(),
                    newMember.passwordHash(),
                    newMember.name(),
                    newMember.phoneNumber(),
                    newMember.createdAt(),
                    newMember.lastLoginAt()
            );
            return member;
        }

        @Override
        public void updateLastLoginAt(Long memberId, LocalDateTime updatedAt) {
            updatedMemberId = memberId;
            lastLoginAt = updatedAt;
        }
    }

    private static class StubPasswordHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hashed-password";
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return "password1".equals(rawPassword) && "hashed-password".equals(passwordHash);
        }
    }
}
