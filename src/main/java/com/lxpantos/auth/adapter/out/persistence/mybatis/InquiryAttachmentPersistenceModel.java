package com.lxpantos.auth.adapter.out.persistence.mybatis;

import java.time.LocalDateTime;

public class InquiryAttachmentPersistenceModel {
    private Long id;
    private Long inquiryId;
    private String storageKey;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private LocalDateTime createdAt;

    public InquiryAttachmentPersistenceModel() { }
    public InquiryAttachmentPersistenceModel(Long id, Long inquiryId, String storageKey, String originalFilename,
                                             String contentType, long fileSize, LocalDateTime createdAt) {
        this.id = id; this.inquiryId = inquiryId; this.storageKey = storageKey; this.originalFilename = originalFilename;
        this.contentType = contentType; this.fileSize = fileSize; this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInquiryId() { return inquiryId; }
    public void setInquiryId(Long inquiryId) { this.inquiryId = inquiryId; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
