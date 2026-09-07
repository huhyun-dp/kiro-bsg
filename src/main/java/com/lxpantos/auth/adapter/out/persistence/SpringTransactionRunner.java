package com.lxpantos.auth.adapter.out.persistence;

import com.lxpantos.auth.application.port.out.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Spring TransactionTemplate 을 이용한 트랜잭션 경계 어댑터.
 * 권한 변경과 감사 로그 저장이 동일 트랜잭션에서 원자적으로 처리되도록 보장한다.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T execute(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }
}
