package com.lxpantos.auth.config;

import com.lxpantos.auth.application.port.out.InquiryQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.application.service.InquiryQueryService;
import com.lxpantos.auth.application.service.InquiryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class InquiryConfiguration {

    @Bean
    InquiryService inquiryService(InquiryRepository inquiryRepository, Clock clock) {
        return new InquiryService(inquiryRepository, clock);
    }

    @Bean
    InquiryQueryService inquiryQueryService(
            InquiryQueryRepository inquiryQueryRepository,
            InquiryRepository inquiryRepository
    ) {
        return new InquiryQueryService(inquiryQueryRepository, inquiryRepository);
    }
}
