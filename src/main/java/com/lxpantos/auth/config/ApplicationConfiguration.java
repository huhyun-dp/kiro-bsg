package com.lxpantos.auth.config;

import com.lxpantos.auth.application.port.out.MemberRepository;
import com.lxpantos.auth.application.port.out.PasswordHasher;
import com.lxpantos.auth.application.service.AuthenticationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuthenticationService authenticationService(
            MemberRepository memberRepository,
            PasswordHasher passwordHasher,
            Clock clock
    ) {
        return new AuthenticationService(memberRepository, passwordHasher, clock);
    }

}
