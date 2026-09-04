package com.lxpantos.auth.adapter.out.persistence.mybatis;

import java.time.LocalDateTime;

public class InquiryPersistenceModel {

    private Long id;
    private Long memberId;
    private String title;
    private String content;
    private Long viewCount;
    private LocalDateTime createdAt;

    public InquiryPersistenceModel() {
    }

    public InquiryPersistenceModel(Long id, Long memberId, String title, String content,
                                   Long viewCount, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
