package com.lxpantos.auth.config;

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

    public MemberQueryConfiguration(ApiAuthenticationInterceptor apiAuthenticationInterceptor) {
        this.apiAuthenticationInterceptor = apiAuthenticationInterceptor;
    }

    @Bean
    MemberQueryService memberQueryService(MemberQueryRepository memberQueryRepository) {
        return new MemberQueryService(memberQueryRepository);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiAuthenticationInterceptor).addPathPatterns("/api/**");
    }
}
