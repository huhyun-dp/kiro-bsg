package com.lxpantos.auth.config;

import com.lxpantos.auth.adapter.in.web.security.AdminPageAuthorizationInterceptor;
import com.lxpantos.auth.adapter.in.web.security.AuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;
    private final AdminPageAuthorizationInterceptor adminPageAuthorizationInterceptor;

    public WebConfiguration(AuthenticationInterceptor authenticationInterceptor,
                            AdminPageAuthorizationInterceptor adminPageAuthorizationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.adminPageAuthorizationInterceptor = adminPageAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 로그인 필요(정지 회원 차단 포함).
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/members", "/members/**", "/inquiries/**")
                .order(10);
        // 회원 관리 화면은 ADMIN 전용(OPERATOR/VIEWER 는 403). 관리자 페이지 인터셉터를 재사용한다.
        registry.addInterceptor(adminPageAuthorizationInterceptor)
                .addPathPatterns("/members", "/members/**")
                .order(20);
    }
}

