package com.lxpantos.auth.application.port.out;

import java.util.function.Supplier;

/**
 * 트랜잭션 경계를 제공하는 아웃바운드 포트.
 * 애플리케이션 서비스가 Spring 트랜잭션 어노테이션에 의존하지 않도록 추상화한다.
 */
public interface TransactionRunner {
    <T> T execute(Supplier<T> action);
}
