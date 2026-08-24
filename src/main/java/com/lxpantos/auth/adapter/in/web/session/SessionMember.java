package com.lxpantos.auth.adapter.in.web.session;

import java.io.Serial;
import java.io.Serializable;

public record SessionMember(Long id, String email, String name) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}

