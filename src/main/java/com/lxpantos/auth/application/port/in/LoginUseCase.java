package com.lxpantos.auth.application.port.in;

public interface LoginUseCase {
    AuthenticatedMember login(LoginCommand command);
}

