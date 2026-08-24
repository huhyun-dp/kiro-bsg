package com.lxpantos.auth.adapter.out.persistence.mybatis;

import com.lxpantos.auth.application.port.out.MemberQueryRepository;
import com.lxpantos.auth.application.port.out.MemberQueryResult;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MyBatisMemberQueryRepository implements MemberQueryRepository {

    private final MemberQueryMapper memberQueryMapper;

    public MyBatisMemberQueryRepository(MemberQueryMapper memberQueryMapper) {
        this.memberQueryMapper = memberQueryMapper;
    }

    @Override
    public List<MemberQueryResult> search(String keyword) {
        return memberQueryMapper.search(toLikePattern(keyword));
    }

    private static final String LIKE_ESCAPE = "!";

    private String toLikePattern(String keyword) {
        if (keyword == null) {
            return null;
        }
        String escaped = keyword
                .replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
