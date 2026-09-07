package com.lxpantos.auth.adapter.in.web.session;

import com.lxpantos.auth.domain.member.MemberRole;

import java.io.Serial;
import java.io.Serializable;

public record SessionMember(Long id, String email, String name, MemberRole role) implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;

    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }
}
