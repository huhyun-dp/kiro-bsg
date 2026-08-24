package com.lxpantos.auth.application.port.in;

import java.util.List;

public interface MemberQueryUseCase {

    List<MemberSummary> search(String keyword);
}
