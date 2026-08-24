package com.lxpantos.auth.application.port.out;

import com.lxpantos.auth.application.port.in.MemberSummary;

import java.util.List;

public interface MemberQueryRepository {
    List<MemberSummary> search(String keyword);
}
