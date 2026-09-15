package com.lxpantos.auth.adapter.in.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public class InquiryForm {
    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 20, message = "제목은 20자 이하로 입력해 주세요.")
    private String title;
    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(max = 500, message = "내용은 500자 이하로 입력해 주세요.")
    private String content;
    private List<MultipartFile> attachments;
    private Set<Long> deleteAttachmentIds;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<MultipartFile> getAttachments() { return attachments; }
    public void setAttachments(List<MultipartFile> attachments) { this.attachments = attachments; }
    public Set<Long> getDeleteAttachmentIds() { return deleteAttachmentIds; }
    public void setDeleteAttachmentIds(Set<Long> deleteAttachmentIds) { this.deleteAttachmentIds = deleteAttachmentIds; }
}
