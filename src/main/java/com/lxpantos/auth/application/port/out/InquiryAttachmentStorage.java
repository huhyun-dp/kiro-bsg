package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.application.port.in.AttachmentContent;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

public interface InquiryAttachmentStorage {
    void store(String storageKey, AttachmentContent content) throws IOException;
    InputStream open(String storageKey) throws IOException;
    boolean exists(String storageKey);
    void delete(String storageKey) throws IOException;
    List<String> findKeysOlderThan(Duration age) throws IOException;
}
