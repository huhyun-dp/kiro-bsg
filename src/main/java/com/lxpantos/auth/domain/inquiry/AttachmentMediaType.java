package com.lxpantos.auth.domain.inquiry;

import java.util.Locale;
import java.util.Set;

public enum AttachmentMediaType {
    PDF("application/pdf", Set.of("pdf")),
    PNG("image/png", Set.of("png")),
    JPEG("image/jpeg", Set.of("jpg", "jpeg"));

    private final String contentType;
    private final Set<String> extensions;

    AttachmentMediaType(String contentType, Set<String> extensions) {
        this.contentType = contentType;
        this.extensions = extensions;
    }

    public String contentType() { return contentType; }

    public boolean supportsExtension(String extension) {
        return extensions.contains(extension.toLowerCase(Locale.ROOT));
    }

    public boolean matchesDeclaredContentType(String declaredContentType) {
        return contentType.equalsIgnoreCase(declaredContentType == null ? "" : declaredContentType);
    }
}
