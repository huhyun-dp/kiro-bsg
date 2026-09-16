package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.port.in.InquiryPage;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryListQueryResult;
import com.lxpantos.auth.application.port.out.InquiryQueryRepository;
import com.lxpantos.auth.domain.inquiry.InquiryAttachment;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryQueryServicePropertyTest {

    @Property(tries = 100)
    // Validates: Requirements 6.1, 6.4, 6.5, 6.6
    void property8_listAttachmentCountIsPreservedWithoutAttachmentReads(
            @ForAll("attachmentCountPatterns") List<Long> attachmentCountPatterns) {
        RecordingListRepository listRepository = new RecordingListRepository(attachmentCountPatterns);
        InquiryQueryService service = new InquiryQueryService(listRepository, null, new FailingAttachmentQueryRepository());

        InquiryPage page = service.getPage(1, 25);

        // 목록 요약은 저장소가 계산한 첨부 개수를 그대로 전달한다.
        assertThat(page.items()).extracting(item -> item.attachmentCount())
                .containsExactlyElementsOf(attachmentCountPatterns);
        // hasAttachments 는 개수 > 0 에서 파생된다.
        assertThat(page.items()).extracting(item -> item.hasAttachments())
                .containsExactlyElementsOf(attachmentCountPatterns.stream().map(count -> count > 0).toList());
        assertThat(listRepository.countAllCalls).isEqualTo(1);
        assertThat(listRepository.findPageCalls).isEqualTo(1);
        // 목록 projection 에는 첨부 식별자·저장 키·경로·다운로드 주소가 없다(개수만 노출).
        assertThat(List.of(InquiryListQueryResult.class.getRecordComponents()))
                .extracting(RecordComponent::getName)
                .containsExactly("id", "title", "viewCount", "createdAt", "attachmentCount");
    }

    @Provide
    Arbitrary<List<Long>> attachmentCountPatterns() {
        return Arbitraries.longs().between(0, 5).list().ofMaxSize(20);
    }

    private static final class RecordingListRepository implements InquiryQueryRepository {
        private final List<InquiryListQueryResult> rows;
        private int countAllCalls;
        private int findPageCalls;

        private RecordingListRepository(List<Long> attachmentCountPatterns) {
            this.rows = IntStream.range(0, attachmentCountPatterns.size())
                    .mapToObj(index -> new InquiryListQueryResult(
                            (long) index + 1,
                            "문의 " + index,
                            0L,
                            LocalDateTime.of(2024, 1, 1, 0, 0).plusMinutes(index),
                            attachmentCountPatterns.get(index)))
                    .toList();
        }

        @Override
        public List<InquiryListQueryResult> findPage(int offset, int limit) {
            findPageCalls++;
            return rows.stream().skip(offset).limit(limit).toList();
        }

        @Override
        public long countAll() {
            countAllCalls++;
            return rows.size();
        }
    }

    private static final class FailingAttachmentQueryRepository implements InquiryAttachmentQueryRepository {
        @Override
        public List<InquiryAttachment> findByInquiryId(Long inquiryId) {
            throw new AssertionError("List queries must not read attachments per inquiry");
        }

        @Override
        public Optional<InquiryAttachment> findByInquiryIdAndId(Long inquiryId, Long attachmentId) {
            throw new AssertionError("List queries must not read attachment metadata");
        }

        @Override
        public Set<String> findAllStorageKeys() {
            throw new AssertionError("List queries must not read attachment storage keys");
        }
    }
}
