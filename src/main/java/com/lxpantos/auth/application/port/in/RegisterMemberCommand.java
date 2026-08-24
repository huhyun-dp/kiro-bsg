package com.lxpantos.auth.application.port.in;

public record RegisterMemberCommand(String email, String password, String name) {
}

