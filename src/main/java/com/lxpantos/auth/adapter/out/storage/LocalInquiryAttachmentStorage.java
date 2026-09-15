package com.lxpantos.auth.adapter.out.storage;

import com.lxpantos.auth.application.port.in.AttachmentContent;
import com.lxpantos.auth.application.port.out.InquiryAttachmentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class LocalInquiryAttachmentStorage implements InquiryAttachmentStorage {
    private static final Logger log = LoggerFactory.getLogger(LocalInquiryAttachmentStorage.class);
    private static final java.util.Set<PosixFilePermission> PRIVATE_DIRECTORY_PERMISSIONS = java.util.Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
    private static final java.util.Set<PosixFilePermission> PRIVATE_FILE_PERMISSIONS = java.util.Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    private final Path root;
    private final Path staging;
    private final Path files;

    public LocalInquiryAttachmentStorage(String configuredRoot) {
        if (configuredRoot == null || configuredRoot.isBlank()) {
            throw new IllegalStateException("INQUIRY_ATTACHMENT_STORAGE_PATH must be configured");
        }
        try {
            Path rawRequested = Path.of(configuredRoot);
            if (!rawRequested.isAbsolute()) {
                throw new IllegalStateException("INQUIRY_ATTACHMENT_STORAGE_PATH must be absolute");
            }
            Path requested = rawRequested.normalize();
            rejectSymlinkedPath(requested);
            Files.createDirectories(requested);
            rejectSymlinkedPath(requested);
            this.root = requested.toRealPath();
            rejectStaticResourceRoot(this.root);
            enforcePrivatePermissions(this.root, PRIVATE_DIRECTORY_PERMISSIONS);
            this.staging = createPrivateDirectory(root.resolve("staging"));
            this.files = createPrivateDirectory(root.resolve("files"));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize inquiry attachment storage", e);
        }
    }

    @Override
    public void store(String storageKey, AttachmentContent content) throws IOException {
        Path finalPath = filePath(storageKey);
        Path stagingPath = stagingPath(storageKey);
        if (Files.exists(finalPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Attachment storage key already exists");
        }
        try (InputStream input = content.openStream()) {
            Files.copy(input, stagingPath, StandardCopyOption.REPLACE_EXISTING);
            enforcePrivatePermissions(stagingPath, PRIVATE_FILE_PERMISSIONS);
            Files.move(stagingPath, finalPath, StandardCopyOption.ATOMIC_MOVE);
            enforcePrivatePermissions(finalPath, PRIVATE_FILE_PERMISSIONS);
        } catch (IOException e) {
            deleteQuietly(stagingPath);
            deleteQuietly(finalPath);
            throw e;
        }
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        Path path = filePath(storageKey);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw new IOException("Attachment is unavailable");
        }
        return Files.newInputStream(path);
    }

    @Override
    public boolean exists(String storageKey) {
        try {
            Path path = filePath(storageKey);
            return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path);
        } catch (IOException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Path path = filePath(storageKey);
        if (Files.isSymbolicLink(path)) {
            throw new IOException("Refusing to delete symbolic link");
        }
        Files.deleteIfExists(path);
    }

    @Override
    public List<String> findKeysOlderThan(Duration age) throws IOException {
        Instant cutoff = Instant.now().minus(age);
        try (Stream<Path> paths = Files.list(files)) {
            return paths.filter(path -> !Files.isSymbolicLink(path))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> isUuid(path.getFileName().toString()))
                    .filter(path -> olderThan(path, cutoff))
                    .map(path -> path.getFileName().toString())
                    .toList();
        }
    }

    private void rejectStaticResourceRoot(Path candidate) throws IOException {
        Path staticRoot = Path.of("src", "main", "resources", "static").toAbsolutePath().normalize();
        if (Files.exists(staticRoot) && candidate.startsWith(staticRoot.toRealPath())) {
            throw new IOException("Attachment storage must not be under static resources");
        }
    }

    private void rejectSymlinkedPath(Path path) throws IOException {
        Path current = path.getRoot();
        for (Path segment : path) {
            current = current == null ? segment : current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IOException("Attachment storage path may not contain symbolic links");
            }
        }
    }

    private void enforcePrivatePermissions(Path path, java.util.Set<PosixFilePermission> permissions) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (view == null) {
            log.warn("Unable to verify POSIX owner-only permissions for inquiry attachment storage: {}. Verify the private volume ACL for the application account.", path);
            return;
        }
        view.setPermissions(permissions);
        java.util.Set<PosixFilePermission> actual = view.readAttributes().permissions();
        if (!actual.equals(permissions)) {
            throw new IOException("Inquiry attachment storage permissions are not owner-only");
        }
    }

    private Path createPrivateDirectory(Path path) throws IOException {
        Path candidate = path.normalize();
        requireInsideRoot(candidate);
        if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(candidate)) {
            throw new IOException("Storage directory may not be a symbolic link");
        }
        Files.createDirectories(candidate);
        Path real = candidate.toRealPath();
        requireInsideRoot(real);
        enforcePrivatePermissions(real, PRIVATE_DIRECTORY_PERMISSIONS);
        return real;
    }

    private Path filePath(String storageKey) throws IOException {
        if (!isUuid(storageKey)) {
            throw new IOException("Invalid attachment storage key");
        }
        Path candidate = files.resolve(storageKey).normalize();
        requireInsideRoot(candidate);
        return candidate;
    }

    private Path stagingPath(String storageKey) throws IOException {
        if (!isUuid(storageKey)) {
            throw new IOException("Invalid attachment storage key");
        }
        Path candidate = staging.resolve(storageKey).normalize();
        requireInsideRoot(candidate);
        return candidate;
    }

    private void requireInsideRoot(Path candidate) throws IOException {
        if (!candidate.startsWith(root)) {
            throw new IOException("Storage path escapes private root");
        }
    }

    private boolean olderThan(Path path, Instant cutoff) {
        try { return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(cutoff); }
        catch (IOException e) { log.warn("Unable to read inquiry attachment timestamp", e); return false; }
    }

    private boolean isUuid(String value) {
        try { return UUID.fromString(value).toString().equals(value); }
        catch (IllegalArgumentException e) { return false; }
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException cleanupFailure) { log.warn("Unable to clean up inquiry attachment file", cleanupFailure); }
    }
}
