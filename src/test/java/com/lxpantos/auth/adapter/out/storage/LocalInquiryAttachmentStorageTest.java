package com.lxpantos.auth.adapter.out.storage;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalInquiryAttachmentStorageTest {
    @TempDir Path tempDir;

    @Test
    void streamsOnlyUuidNamedFilesInsidePrivateRoot() throws Exception {
        LocalInquiryAttachmentStorage storage = new LocalInquiryAttachmentStorage(tempDir.toAbsolutePath().toString());
        String key = UUID.randomUUID().toString();

        storage.store(key, () -> new ByteArrayInputStream("safe".getBytes()));

        assertThat(storage.open(key).readAllBytes()).isEqualTo("safe".getBytes());
        assertThat(Files.exists(tempDir.resolve("files").resolve(key))).isTrue();
        assertThatThrownBy(() -> storage.open("../outside")).isInstanceOf(Exception.class);
        storage.delete(key);
        assertThat(storage.exists(key)).isFalse();
    }

    @Test
    void rejectsConfiguredRootThatTraversesASymbolicLink() throws Exception {
        Path actualRoot = tempDir.resolve("actual-root");
        Files.createDirectories(actualRoot);
        Path linkedRoot = tempDir.resolve("linked-root");
        try {
            Files.createSymbolicLink(linkedRoot, actualRoot);
        } catch (UnsupportedOperationException | IOException exception) {
            Assumptions.abort("Symbolic links are unavailable in this test environment");
        }

        assertThatThrownBy(() -> new LocalInquiryAttachmentStorage(linkedRoot.toAbsolutePath().toString()))
                .isInstanceOf(IllegalStateException.class);
    }
}
