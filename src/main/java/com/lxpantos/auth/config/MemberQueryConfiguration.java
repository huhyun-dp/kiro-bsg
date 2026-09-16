package com.lxpantos.auth.config;

import com.lxpantos.auth.adapter.in.web.security.AdminApiAuthorizationInterceptor;
import com.lxpantos.auth.adapter.in.web.security.ApiAuthenticationInterceptor;
import com.lxpantos.auth.application.port.out.MemberQueryRepository;
import com.lxpantos.auth.application.service.MemberQueryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class MemberQueryConfiguration implements WebMvcConfigurer {

    private final ApiAuthenticationInterceptor apiAuthenticationInterceptor;
    private final AdminApiAuthorizationInterceptor adminApiAuthorizationInterceptor;

    public MemberQueryConfiguration(ApiAuthenticationInterceptor apiAuthenticationInterceptor,
                                    AdminApiAuthorizationInterceptor adminApiAuthorizationInterceptor) {
        this.apiAuthenticationInterceptor = apiAuthenticationInterceptor;
        this.adminApiAuthorizationInterceptor = adminApiAuthorizationInterceptor;
    }

    @Bean
    MemberQueryService memberQueryService(MemberQueryRepository memberQueryRepository) {
        return new MemberQueryService(memberQueryRepository);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 모든 /api/** 는 인증 필요(정지 회원 차단 포함).
        registry.addInterceptor(apiAuthenticationInterceptor).addPathPatterns("/api/**").order(10);
        // 회원 목록 API 는 ADMIN 전용(OPERATOR/VIEWER 는 403). 관리자 API 인터셉터를 재사용한다.
        registry.addInterceptor(adminApiAuthorizationInterceptor)
                .addPathPatterns("/api/members", "/api/members/**")
                .order(20);
    }
}
