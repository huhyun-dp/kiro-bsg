package com.lxpantos.auth.config;

import com.lxpantos.auth.application.port.out.AccessMemberQueryRepository;
import com.lxpantos.auth.application.port.out.AccessMemberRepository;
import com.lxpantos.auth.application.port.out.TransactionRunner;
import com.lxpantos.auth.application.service.AccessManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * 권한 관리 서비스/트랜잭션 빈 조립.
 * 인터셉터 등록은 순환 참조를 피하기 위해 {@link AccessManagementWebConfiguration} 에서 별도로 수행한다.
 */
@Configuration
public class AccessManagementConfiguration {

    @Bean
    TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    AccessManagementService accessManagementService(
            AccessMemberRepository accessMemberRepository,
            AccessMemberQueryRepository accessMemberQueryRepository,
            TransactionRunner transactionRunner,
            Clock clock
    ) {
        return new AccessManagementService(
                accessMemberRepository,
                accessMemberQueryRepository,
                transactionRunner,
                clock
        );
    }
}
