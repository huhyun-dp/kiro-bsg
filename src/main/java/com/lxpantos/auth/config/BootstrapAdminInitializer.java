package com.lxpantos.auth.config;

import com.lxpantos.auth.application.port.out.AccessMemberRepository;
import com.lxpantos.auth.application.port.out.TransactionRunner;
import com.lxpantos.auth.domain.member.Member;
import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * 운영 환경에서 초기 관리자를 지정하기 위한 부트스트랩.
 * {@code BOOTSTRAP_ADMIN_EMAIL} 로 지정한 회원을 앱 시작 시 ADMIN/ACTIVE 로 승격한다.
 *
 * <ul>
 *   <li>값이 비어 있으면 아무 것도 하지 않는다.</li>
 *   <li>이미 활성 ADMIN 이면 변경하지 않는다(멱등성).</li>
 *   <li>대상 회원이 없으면 경고만 남기고 진행한다(로그인 정책을 막지 않음).</li>
 * </ul>
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final AccessMemberRepository accessMemberRepository;
    private final TransactionRunner transactionRunner;
    private final Clock clock;
    private final String bootstrapAdminEmail;

    public BootstrapAdminInitializer(
            AccessMemberRepository accessMemberRepository,
            TransactionRunner transactionRunner,
            Clock clock,
            @Value("${app.bootstrap-admin-email:${BOOTSTRAP_ADMIN_EMAIL:}}") String bootstrapAdminEmail
    ) {
        this.accessMemberRepository = accessMemberRepository;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapAdminEmail == null || bootstrapAdminEmail.isBlank()) {
            return;
        }
        String email = bootstrapAdminEmail.strip().toLowerCase(Locale.ROOT);

        transactionRunner.execute(() -> {
            Optional<Member> found = accessMemberRepository.findByEmail(email);
            if (found.isEmpty()) {
                log.warn("BOOTSTRAP_ADMIN_EMAIL '{}' 회원을 찾을 수 없어 관리자 승격을 건너뜁니다.", email);
                return null;
            }
            Member member = found.get();
            if (member.role() == MemberRole.ADMIN && member.status() == MemberStatus.ACTIVE) {
                return null;
            }
            int updated = accessMemberRepository.updateAccess(
                    member.id(),
                    MemberRole.ADMIN,
                    MemberStatus.ACTIVE,
                    LocalDateTime.now(clock),
                    member.version()
            );
            if (updated > 0) {
                log.info("BOOTSTRAP_ADMIN_EMAIL '{}' 회원을 활성 관리자로 승격했습니다.", email);
            }
            return null;
        });
    }
}
