package com.lxpantos.auth.application.port.in;

import com.lxpantos.auth.domain.member.MemberRole;

public record AuthenticatedMember(Long id, String email, String name, MemberRole role) {
}
