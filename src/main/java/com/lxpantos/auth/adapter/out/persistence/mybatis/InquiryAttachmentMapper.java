package com.lxpantos.auth.adapter.out.persistence.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface InquiryAttachmentMapper {
    void insert(InquiryAttachmentPersistenceModel model);
    List<InquiryAttachmentPersistenceModel> findByInquiryId(@Param("inquiryId") Long inquiryId);
    Optional<InquiryAttachmentPersistenceModel> findByInquiryIdAndId(@Param("inquiryId") Long inquiryId,
                                                                       @Param("attachmentId") Long attachmentId);
    int deleteByInquiryIdAndId(@Param("inquiryId") Long inquiryId, @Param("attachmentId") Long attachmentId);
    List<String> findAllStorageKeys();
}
