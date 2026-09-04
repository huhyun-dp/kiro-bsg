package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.port.in.CreateInquiryCommand;
import com.lxpantos.auth.application.port.in.CreateInquiryUseCase;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.domain.inquiry.Inquiry;

import java.time.Clock;
import java.time.LocalDateTime;

public class InquiryService implements CreateInquiryUseCase {

    private final InquiryRepository inquiryRepository;
    private final Clock clock;

    public InquiryService(InquiryRepository inquiryRepository, Clock clock) {
        this.inquiryRepository = inquiryRepository;
        this.clock = clock;
    }

    @Override
    public Long create(CreateInquiryCommand command) {
        Inquiry inquiry = new Inquiry(
                null,
                command.memberId(),
                command.title(),
                command.content(),
                0L,
                LocalDateTime.now(clock)
        );
        return inquiryRepository.save(inquiry);
    }
}
