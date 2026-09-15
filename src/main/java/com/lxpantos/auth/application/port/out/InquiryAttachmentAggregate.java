package com.lxpantos.auth.application.port.out;

/**
 * 문의에 연결된 첨부 메타데이터의 저장 시점 집계다.
 * 파일 시스템이나 MyBatis 타입을 노출하지 않는다.
 */
public record InquiryAttachmentAggregate(long count, long totalFileSize) {
}
