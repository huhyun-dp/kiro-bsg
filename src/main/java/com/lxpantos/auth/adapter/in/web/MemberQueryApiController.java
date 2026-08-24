package com.lxpantos.auth.adapter.in.web;

import com.lxpantos.auth.application.port.in.MemberQueryUseCase;
import com.lxpantos.auth.application.port.in.MemberSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberQueryApiController {

    private final MemberQueryUseCase memberQueryUseCase;

    public MemberQueryApiController(MemberQueryUseCase memberQueryUseCase) {
        this.memberQueryUseCase = memberQueryUseCase;
    }

    @GetMapping
    public List<MemberSummary> search(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return memberQueryUseCase.search(keyword);
    }
}
