package com.lxpantos.auth.application.port.in;

public record AuthenticatedMember(Long id, String email, String name) {
}

