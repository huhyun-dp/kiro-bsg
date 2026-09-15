package com.lxpantos.auth.application.port.in;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface AttachmentContent {
    InputStream openStream() throws IOException;
}
