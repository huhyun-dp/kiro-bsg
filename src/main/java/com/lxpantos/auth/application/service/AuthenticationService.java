package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.exception.DuplicateEmailException;
import com.lxpantos.auth.application.exception.InvalidCredentialsException;
import com.lxpantos.auth.application.port.in.AuthenticatedMember;
import com.lxpantos.auth.application.port.in.LoginCommand;
import com.lxpantos.auth.application.port.in.LoginUseCase;
import com.lxpantos.auth.application.port.in.RegisterMemberCommand;
import com.lxpantos.auth.application.port.in.RegisterMemberUseCase;
import com.lxpantos.auth.application.port.out.MemberRepository;
import com.lxpantos.auth.application.port.out.PasswordHasher;
import com.lxpantos.auth.domain.member.Member;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class AuthenticationService implements RegisterMemberUseCase, LoginUseCase {

    // 존재하지 않는 계정도 BCrypt 비교를 수행해 응답 시간으로 계정 존재 여부가 드러나는 것을 줄인다.
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.";
    private static final Pattern MOBILE_PHONE_PATTERN = Pattern.compile("^010\\d{8}$");

    private final MemberRepository memberRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AuthenticationService(MemberRepository memberRepository, PasswordHasher passwordHasher, Clock clock) {
        this.memberRepository = memberRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public Long register(RegisterMemberCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String email = normalizeEmail(command.email());
        String name = Objects.requireNonNull(command.name(), "name must not be null").strip();
        String phoneNumber = normalizePhoneNumber(command.phoneNumber());

        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        Member saved = memberRepository.save(Member.newMember(
                email,
                passwordHasher.hash(command.password()),
                name,
                phoneNumber,
                LocalDateTime.now(clock)
        ));
        return saved.id();
    }

    @Override
    public AuthenticatedMember login(LoginCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String email = normalizeEmail(command.email());
        Member member = memberRepository.findByEmail(email).orElse(null);

        if (member == null) {
            passwordHasher.matches(command.password(), DUMMY_PASSWORD_HASH);
            throw new InvalidCredentialsException();
        }
        if (!passwordHasher.matches(command.password(), member.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        memberRepository.updateLastLoginAt(member.id(), LocalDateTime.now(clock));
        return new AuthenticatedMember(member.id(), member.email(), member.name());
    }

    private String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email must not be null").strip().toLowerCase(Locale.ROOT);
    }

    private String normalizePhoneNumber(String phoneNumber) {
        String normalized = Objects.requireNonNull(phoneNumber, "phoneNumber must not be null")
                .replaceAll("[^0-9]", "");
        if (!MOBILE_PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("phoneNumber must be a valid Korean mobile number");
        }
        return normalized;
    }
}
