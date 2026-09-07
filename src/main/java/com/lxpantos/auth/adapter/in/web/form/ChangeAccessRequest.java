package com.lxpantos.auth.adapter.in.web.form;

import com.lxpantos.auth.domain.member.MemberRole;
import com.lxpantos.auth.domain.member.MemberStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 권한/상태 변경 API 요청 바디. 서버에서 규칙을 다시 검증하므로 여기서는 기본 형식만 검사한다.
 */
public class ChangeAccessRequest {

    @NotNull(message = "역할을 선택해 주세요.")
    private MemberRole role;

    @NotNull(message = "상태를 선택해 주세요.")
    private MemberStatus status;

    @Size(min = 4, max = 500, message = "변경 사유는 4자 이상 500자 이하로 입력해 주세요.")
    private String reason;

    @NotNull(message = "요청 버전이 필요합니다.")
    @PositiveOrZero(message = "요청 버전이 올바르지 않습니다.")
    private Long expectedVersion;

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
