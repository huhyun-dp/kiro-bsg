package com.lxpantos.auth.application.service;

import com.lxpantos.auth.application.port.out.InquiryAttachmentQueryRepository;
import com.lxpantos.auth.application.port.out.InquiryAttachmentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class InquiryAttachmentOrphanCleanupService {
    private static final Logger log = LoggerFactory.getLogger(InquiryAttachmentOrphanCleanupService.class);
    private final InquiryAttachmentStorage storage;
    private final InquiryAttachmentQueryRepository attachmentQueryRepository;

    public InquiryAttachmentOrphanCleanupService(InquiryAttachmentStorage storage,
                                                  InquiryAttachmentQueryRepository attachmentQueryRepository) {
        this.storage = storage;
        this.attachmentQueryRepository = attachmentQueryRepository;
    }

    public int cleanup(Duration age) {
        Set<String> referenced = new HashSet<>(attachmentQueryRepository.findAllStorageKeys());
        int deleted = 0;
        try {
            for (String key : storage.findKeysOlderThan(age)) {
                if (!referenced.contains(key)) {
                    try {
                        storage.delete(key);
                        deleted++;
                    } catch (IOException e) {
                        log.warn("Unable to remove orphan inquiry attachment", e);
                    }
                }
            }
            return deleted;
        } catch (IOException e) {
            log.warn("Unable to scan inquiry attachment storage for orphans", e);
            return deleted;
        }
    }
}
