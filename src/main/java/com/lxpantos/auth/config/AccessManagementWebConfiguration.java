package com.lxpantos.auth.config;

import com.lxpantos.auth.adapter.in.web.security.AdminApiAuthorizationInterceptor;
import com.lxpantos.auth.adapter.in.web.security.AdminPageAuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 관리자 전용 경로 인터셉터 등록.
 * 인터셉터는 @Component 로 생성되며 여기서는 경로 매핑만 담당한다(빈 정의 없음).
 */
@Configuration
public class AccessManagementWebConfiguration implements WebMvcConfigurer {

    private final AdminPageAuthorizationInterceptor adminPageAuthorizationInterceptor;
    private final AdminApiAuthorizationInterceptor adminApiAuthorizationInterceptor;

    public AccessManagementWebConfiguration(
            AdminPageAuthorizationInterceptor adminPageAuthorizationInterceptor,
            AdminApiAuthorizationInterceptor adminApiAuthorizationInterceptor
    ) {
        this.adminPageAuthorizationInterceptor = adminPageAuthorizationInterceptor;
        this.adminApiAuthorizationInterceptor = adminApiAuthorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminApiAuthorizationInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(20);
        registry.addInterceptor(adminPageAuthorizationInterceptor)
                .addPathPatterns("/admin/**")
                .order(20);
    }
}
