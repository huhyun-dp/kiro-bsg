package com.lxpantos.auth.adapter.out.persistence.mybatis;

import java.time.LocalDateTime;

public class AuditLogPersistenceModel {
    private Long id;
    private Long targetMemberId;
    private Long actorMemberId;
    private String beforeRole;
    private String beforeStatus;
    private String afterRole;
    private String afterStatus;
    private String reason;
    private String requestIp;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTargetMemberId() {
        return targetMemberId;
    }

    public void setTargetMemberId(Long targetMemberId) {
        this.targetMemberId = targetMemberId;
    }

    public Long getActorMemberId() {
        return actorMemberId;
    }

    public void setActorMemberId(Long actorMemberId) {
        this.actorMemberId = actorMemberId;
    }

    public String getBeforeRole() {
        return beforeRole;
    }

    public void setBeforeRole(String beforeRole) {
        this.beforeRole = beforeRole;
    }

    public String getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(String beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public String getAfterRole() {
        return afterRole;
    }

    public void setAfterRole(String afterRole) {
        this.afterRole = afterRole;
    }

    public String getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(String afterStatus) {
        this.afterStatus = afterStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
