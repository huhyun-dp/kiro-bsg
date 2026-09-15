package com.lxpantos.auth.config;

import com.lxpantos.auth.adapter.out.storage.LocalInquiryAttachmentStorage;
import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentStorage;
import com.lxpantos.auth.application.port.out.InquiryQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryRepository;
import com.lxpantos.auth.application.port.out.TransactionRunner;
import com.lxpantos.auth.application.port.in.DeleteInquiryAttachmentUseCase;
import com.lxpantos.auth.application.service.InquiryAttachmentDownloadService;
import com.lxpantos.auth.application.service.InquiryAttachmentOrphanCleanupService;
import com.lxpantos.auth.application.service.InquiryQueryService;
import com.lxpantos.auth.application.service.InquiryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class InquiryConfiguration {
    @Bean
    InquiryAttachmentStorage inquiryAttachmentStorage(@Value("${app.inquiry-attachment.storage-path}") String storagePath) {
        return new LocalInquiryAttachmentStorage(storagePath);
    }
    @Bean
    InquiryService inquiryService(InquiryRepository inquiryRepository, InquiryAttachmentRepository attachmentRepository,
                                  InquiryAttachmentQueryRepository attachmentQueryRepository, InquiryAttachmentStorage attachmentStorage,
                                  TransactionRunner transactionRunner, Clock clock) {
        return new InquiryService(inquiryRepository, attachmentRepository, attachmentQueryRepository, attachmentStorage, transactionRunner, clock);
    }
    @Bean
    DeleteInquiryAttachmentUseCase deleteInquiryAttachmentUseCase(InquiryService inquiryService) {
        return inquiryService;
    }
    @Bean
    InquiryQueryService inquiryQueryService(InquiryQueryRepository inquiryQueryRepository, InquiryRepository inquiryRepository,
                                            InquiryAttachmentQueryRepository attachmentQueryRepository) {
        return new InquiryQueryService(inquiryQueryRepository, inquiryRepository, attachmentQueryRepository);
    }
    @Bean
    InquiryAttachmentDownloadService inquiryAttachmentDownloadService(InquiryAttachmentQueryRepository attachmentQueryRepository,
                                                                       InquiryAttachmentStorage attachmentStorage) {
        return new InquiryAttachmentDownloadService(attachmentQueryRepository, attachmentStorage);
    }
    @Bean
    InquiryAttachmentOrphanCleanupService inquiryAttachmentOrphanCleanupService(InquiryAttachmentStorage attachmentStorage,
                                                                                  InquiryAttachmentQueryRepository attachmentQueryRepository) {
        return new InquiryAttachmentOrphanCleanupService(attachmentStorage, attachmentQueryRepository);
    }
    @Bean
    @ConditionalOnProperty(name = "app.inquiry-attachment.cleanup-on-startup", havingValue = "true")
    ApplicationRunner inquiryAttachmentCleanupRunner(InquiryAttachmentOrphanCleanupService cleanupService) {
        return args -> cleanupService.cleanup(Duration.ofHours(24));
    }
}
