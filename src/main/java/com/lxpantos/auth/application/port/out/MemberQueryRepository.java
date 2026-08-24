package com.lxpantos.auth.application.port.out;

import java.util.List;

public interface MemberQueryRepository {
    List<MemberQueryResult> search(String keyword);
}
