package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.port.in.CreateInquiryCommand;
import com.lxpantos.auth.application.port.out.InquiryQueryResult;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.domain.inquiry.Inquiry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryServiceTest {

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2024-06-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private InquiryRepository fakeRepository;
    private InquiryService service;

    private final AtomicLong idSequence = new AtomicLong(1);
    private final AtomicReference<Inquiry> savedInquiry = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        fakeRepository = new InquiryRepository() {
            @Override
            public Long save(Inquiry inquiry) {
                long id = idSequence.getAndIncrement();
                savedInquiry.set(new Inquiry(id, inquiry.memberId(), inquiry.title(),
                        inquiry.content(), inquiry.viewCount(), inquiry.createdAt()));
                return id;
            }

            @Override
            public void incrementViewCount(Long id) { }

            @Override
            public Optional<InquiryQueryResult> findById(Long id) {
                return Optional.empty();
            }
        };
        service = new InquiryService(fakeRepository, fixedClock);
    }

    @Test
    void savesInquiryWithCorrectFields() {
        CreateInquiryCommand command = new CreateInquiryCommand(42L, "테스트 제목", "테스트 내용");

        Long returnedId = service.create(command);

        Inquiry saved = savedInquiry.get();
        assertThat(returnedId).isEqualTo(1L);
        assertThat(saved.memberId()).isEqualTo(42L);
        assertThat(saved.title()).isEqualTo("테스트 제목");
        assertThat(saved.content()).isEqualTo("테스트 내용");
        assertThat(saved.viewCount()).isZero();
        assertThat(saved.createdAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }
}
