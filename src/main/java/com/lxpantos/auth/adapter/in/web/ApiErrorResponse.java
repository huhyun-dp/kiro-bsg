package com.lxpantos.auth.adapter.in.web;

/**
 * 관리자 API 의 일관된 오류 응답 구조. 상세 원인은 노출하지 않고 한국어 메시지만 담는다.
 */
public record ApiErrorResponse(int status, String message) {
}
