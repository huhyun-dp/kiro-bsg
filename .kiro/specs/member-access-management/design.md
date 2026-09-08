# 권한 관리(Access Management) 설계 문서

> **문서 목적:** 권한 관리 기능의 구현 기준 업무 흐름 및 구조 설명 (기존 `member-auth-baseline` 스펙과 분리 관리)
> **표기 규칙:**
> - ✅ 확인됨 — 실제 구현 코드에서 직접 확인한 사실
> - 🔶 추정 — 구현을 근거로 판단했으나 담당자 확인 권장
> - ❓ 확인 필요

---

## Overview

**권한 관리 기능**은 기존 BSG Partners 회원관리 시스템에 역할 기반 접근 제어(RBAC)와 감사 로그를 추가한 확장이다. 회원은 역할(`ADMIN`/`OPERATOR`/`VIEWER`)과 계정 상태(`ACTIVE`/`SUSPENDED`)를 가지며, `ADMIN`만 `/admin/access` 화면과 `/api/admin/**` API로 다른 회원의 역할·상태를 변경할 수 있다. 모든 변경은 낙관적 잠금으로 동시성을 통제하고, 변경과 동일한 트랜잭션에서 감사 로그로 기록된다.

이 기능은 기존 스펙([`member-auth-baseline`](../member-auth-baseline/requirements.md))의 확인 항목 **Q1**(권한 구분 필요 여부)에 대한 응답으로 도입되었다. 기존 인증·세션·회원 조회 기능은 그대로 유지되며, 본 기능은 그 위에 얹혀 동작한다.

| 사용 대상 | 권한 관리 접근 |
|-----------|----------------|
| 미로그인 방문자 | 접근 불가 → 로그인 화면 |
| `VIEWER` / `OPERATOR` | 메뉴 미노출, 화면 403 / API 403 |
| `ADMIN` | 권한 관리 화면·API 사용 가능 |
| `SUSPENDED`(역할 무관) | 로그인 불가, 기존 세션은 다음 요청에서 차단 |

**기술 스택:** 기존 시스템과 동일(Spring Boot, Thymeleaf, MyBatis, MySQL, Vanilla JS, TOAST UI Grid). 신규 프런트엔드 프레임워크는 도입하지 않았다. DB 스키마는 **Liquibase** changeSet으로 관리한다.

---

## Architecture

기존 시스템의 **헥사고날 아키텍처(Ports & Adapters)**를 그대로 따른다. 쓰기(변경)와 읽기(조회)를 CQRS로 분리하고, 권한 변경과 감사 로그 저장은 `TransactionRunner` 포트로 동일 트랜잭션을 보장한다.

```
[브라우저 / 관리자]
        │  /admin/access (화면),  /api/admin/** (AJAX)
        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Adapter (in / web)                            │
│  AccessManagementPageController   — GET /admin/access          │
│  AccessManagementApiController    — GET/PUT /api/admin/members │
│  AccessManagementApiExceptionHandler — 예외 → 상태/JSON 매핑    │
│                                                                │
│  [인터셉터]                                                     │
│  ApiAuthenticationInterceptor        — /api/** 인증 + 정지 차단 │
│  AdminApiAuthorizationInterceptor    — /api/admin/** ADMIN(401/403)│
│  AdminPageAuthorizationInterceptor   — /admin/** ADMIN(403)     │
│  AuthenticationInterceptor           — /members/** 인증 + 정지 차단│
│  CsrfTokenFilter                     — 상태 변경 요청 CSRF 검증  │
└─────────────────────────────────────────────────────────────┘
        │  port/in (Use Case)
        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Application (service)                         │
│  AccessManagementService                                       │
│    - AccessManagementUseCase   (검색 / 변경 / 감사 로그)        │
│    - MemberAccessLookupUseCase (세션 검증용 역할·상태 조회)      │
│  AuthenticationService — 로그인 시 SUSPENDED 차단(확장)         │
└─────────────────────────────────────────────────────────────┘
        │  port/out (Repository / Service)
        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Adapter (out / persistence)                   │
│  MyBatisAccessMemberRepository       — 낙관적 잠금 update, 감사 로그 insert│
│  MyBatisAccessMemberQueryRepository  — 검색/카운트/감사 로그 조회│
│  SpringTransactionRunner             — TransactionTemplate 트랜잭션 경계│
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│                       Database (MySQL)                         │
│  members (role/status/version/role_updated_at 확장)            │
│  member_access_audit_log (감사 로그)                           │
│  — Liquibase changeSet으로 스키마 관리                         │
└─────────────────────────────────────────────────────────────┘
```

> **의존성 규칙:** `domain`은 어디에도 의존하지 않고, `application/service`는 `domain`과 `port`에만 의존하며, `adapter`는 `port`를 통해서만 서비스와 통신한다. `config`가 모든 빈을 수동 조립한다(애플리케이션 서비스에는 Spring 어노테이션 없음).

### 주요 업무 흐름

#### 2.1 권한 관리 화면 진입 및 목록 조회

```
[사이드바] ADMIN 에게만 "권한 관리" 노출
  │  클릭 → GET /admin/access
  │
  ├─ (미인증 → 로그인 화면 / 비관리자 → HTTP 403 오류 화면)
  │
  ├─ 화면 로드 후 AJAX: GET /api/admin/members?page=0&size=20
  │     └─ 그리드 컬럼: 번호 / 이름 / 이메일 / 휴대폰(마스킹) / 역할 / 상태 / 변경 일시 / [변경]
  │
  ├─ 검색어(이름·이메일·휴대폰) + 역할 필터 + 상태 필터 → [조회]
  │     └─ 서버 사이드 페이지네이션, 전체 건수(총 N명) 표시
  │
  └─ 상태 표시: 빈 결과("조회된 회원이 없습니다."), 오류(네트워크/403) 메시지
```

#### 2.2 역할·상태 변경 흐름

```
[목록] 대상 회원 행의 [변경] 버튼 클릭
  │
  ├─ 변경 모달 표시 (역할 select / 상태 select / 변경 사유 textarea)
  │     └─ 동시에 해당 회원 감사 로그를 모달 하단에 로드
  │
  ├─ [변경] 클릭 → 사유 4자 이상 클라이언트 확인 → 저장 확인 다이얼로그
  │
  ├─ [저장] → PUT /api/admin/members/{id}/access
  │     { role, status, reason, expectedVersion }
  │
  ├─ 성공(200) → 목록 갱신, "변경이 저장되었습니다.", 감사 로그 재조회
  ├─ 입력 오류(400) → 모달에 오류 메시지 (사유 길이 등)
  ├─ 권한 규칙 위반(400) → 본인 강등/정지, 마지막 관리자 보호 등 메시지
  ├─ 충돌(409) → "다른 관리자가 먼저 변경했습니다" + 목록 최신화 안내
  └─ 없음(404) → 대상 회원 없음 안내
```

#### 2.3 정지 회원 세션 차단 흐름

```
[관리자] 회원 A 를 SUSPENDED 로 변경
  │
[회원 A] 기존 로그인 세션으로 다음 요청 시도
  │
  ├─ 인터셉터가 최신 DB 상태 조회 → status = SUSPENDED 감지
  │
  ├─ 세션 무효화(invalidate)
  ├─ 화면 경로(/members, /admin) → 로그인 화면으로 이동
  └─ API 경로(/api/**)          → HTTP 401 JSON
```

---

## Components and Interfaces

### 화면·API 경로 (URL Endpoints)

| 경로 | 메서드 | 권한 | 설명 |
|------|--------|------|------|
| `/admin/access` | GET | ADMIN | 권한 관리 화면 |
| `/api/admin/members` | GET | ADMIN | 대상 회원 목록(검색·역할/상태 필터·페이지네이션) |
| `/api/admin/members/{id}/access` | PUT | ADMIN | 회원 역할/상태 변경(감사 로그 기록, CSRF 필요) |
| `/api/admin/members/{id}/audit-logs` | GET | ADMIN | 회원 권한 변경 감사 로그(최신순 페이지네이션) |

### 접근 제어 컴포넌트

| 컴포넌트 | 역할 | 처리 방식 |
|---------|------|-----------|
| `ApiAuthenticationInterceptor` | `/api/**` 인증 | 미인증 401, 정지 회원 세션 무효화 후 401 |
| `AdminApiAuthorizationInterceptor` | `/api/admin/**` 권한 | 미인증 401, 정지 401, 비관리자 403 (JSON) |
| `AdminPageAuthorizationInterceptor` | `/admin/**` 권한 | 미인증 로그인 이동, 정지 세션 무효화, 비관리자 403 |
| `AuthenticationInterceptor` | `/members/**` 인증 | 미인증 로그인 이동, 정지 회원 세션 무효화 후 로그인 이동 |
| `CsrfTokenFilter` | 상태 변경 요청 CSRF | 비안전 메서드 토큰 검증. 불일치 시 403 |

> **인터셉터 순서:** `/api/admin/**` 요청은 `ApiAuthenticationInterceptor`(기본 order 0)가 먼저 실행되어 미인증 401을 처리하고, 이어 `AdminApiAuthorizationInterceptor`(order 20)가 비관리자 403을 처리한다. 두 인터셉터 모두 권한을 세션이 아닌 최신 DB 상태로 판단한다. ✅

### 주요 파일 위치

| 역할 | 경로 |
|------|------|
| 권한 관리 화면 컨트롤러 | `adapter/in/web/AccessManagementPageController.java` |
| 권한 관리 API 컨트롤러 | `adapter/in/web/AccessManagementApiController.java` |
| API 예외 → 상태/JSON 매핑 | `adapter/in/web/AccessManagementApiExceptionHandler.java` |
| 변경 요청 DTO | `adapter/in/web/form/ChangeAccessRequest.java` |
| 권한 관리 업무 규칙 | `application/service/AccessManagementService.java` |
| 트랜잭션 경계 | `adapter/out/persistence/SpringTransactionRunner.java` |
| 쓰기 쿼리(낙관적 잠금·감사 로그) | `resources/mapper/AccessMemberMapper.xml` |
| 읽기 쿼리(검색·감사 로그) | `resources/mapper/AccessMemberQueryMapper.xml` |
| 관리자 인터셉터 | `adapter/in/web/security/Admin{Api,Page}AuthorizationInterceptor.java` |
| 초기 관리자 승격 | `config/BootstrapAdminInitializer.java` |
| DB 스키마 이력 | `resources/db/changelog/db.changelog-master.yaml` |
| 화면 템플릿 | `resources/templates/admin/access.html`, `resources/templates/fragments/layout.html` |
| 화면 스크립트 | `resources/static/js/access.js` |

### 권한 관리 화면 UI 구성 (`/admin/access`)

레이아웃: 기존 회원 관리 화면과 동일한 상단 헤더 + 좌측 사이드바 · 본문 2단 구성(공통 fragment 재사용).

**본문 영역:**
1. 페이지 헤더: 영문 라벨 `ACCESS` / 타이틀 `권한 관리`
2. **검색·필터 카드:** 검색어 입력(이름·이메일·휴대폰) + 역할 select + 상태 select + [조회]
3. **회원 목록 카드:** 헤딩 `회원 목록` + 총 인원(`총 N명`) + 상태 메시지 영역 + TOAST UI Grid + 페이지네이션(이전 / `N / M` / 다음)

그리드 컬럼 구성:

| 컬럼 헤더 | 너비 | 비고 |
|-----------|------|------|
| 번호 | 70px | |
| 이름 | 최소 110px | |
| 이메일 | 최소 200px | |
| 휴대폰 번호 | 최소 140px | 중간 4자리 `****` 처리 |
| 역할 | 110px | ADMIN / OPERATOR / VIEWER |
| 상태 | 90px | 활성 / 정지 |
| 변경 일시 | 최소 170px | `YYYY-MM-DD HH:MM:SS`, 미변경 시 `-` |
| 관리 | 90px | [변경] 버튼 |

> 회원 관리 그리드와 동일한 TOAST UI Grid 테마를 적용해 행마다 구분선을 표시한다. 페이지 이동 시 상태 표시줄 높이를 고정하고 요청 중 기존 행을 유지하여 깜빡임을 방지한다. ✅

**권한 변경 모달:** 대상 회원(이름·이메일) + 역할 select + 상태 select + 변경 사유 textarea + [취소]/[변경] + 하단 "최근 변경 이력"(감사 로그) 영역 + 이력 페이지네이션. 키보드 조작(Esc 닫기), `label`/`aria` 속성, 포커스 관리, 모바일 대응을 포함한다.

**저장 확인 다이얼로그:** 변경 내용을 요약해 표시하고 [돌아가기]/[저장]으로 확인받는다.

---

## Data Models

### 회원 정보 확장 (`members` 테이블)

기존 컬럼(`id`, `email`, `password_hash`, `name`, `phone_number`, `created_at`, `last_login_at`)에 다음이 추가되었다. Liquibase changeSet이 컬럼 존재 여부를 precondition으로 감지해 누락분만 추가한다.

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `role` | VARCHAR(20) | NOT NULL, 기본값 `VIEWER` | 역할: `ADMIN`/`OPERATOR`/`VIEWER` |
| `status` | VARCHAR(20) | NOT NULL, 기본값 `ACTIVE` | 계정 상태: `ACTIVE`/`SUSPENDED` |
| `version` | BIGINT | NOT NULL, 기본값 0 | 낙관적 잠금 버전 |
| `role_updated_at` | DATETIME(6) | NULL 허용 | 권한 최종 변경 일시(Asia/Seoul) |

### 감사 로그 (`member_access_audit_log` 테이블)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT AUTO_INCREMENT | PK | 로그 식별 번호 |
| `target_member_id` | BIGINT | NOT NULL | 변경 대상 회원 |
| `actor_member_id` | BIGINT | NOT NULL | 변경 수행자(관리자) |
| `before_role` | VARCHAR(20) | NOT NULL | 변경 전 역할 |
| `before_status` | VARCHAR(20) | NOT NULL | 변경 전 상태 |
| `after_role` | VARCHAR(20) | NOT NULL | 변경 후 역할 |
| `after_status` | VARCHAR(20) | NOT NULL | 변경 후 상태 |
| `reason` | VARCHAR(500) | NOT NULL | 변경 사유 |
| `request_ip` | VARCHAR(45) | NULL 허용 | 요청 IP(조회 시 마스킹 표시) |
| `created_at` | DATETIME(6) | NOT NULL | 변경 일시(Asia/Seoul) |

인덱스: `idx_audit_target_created (target_member_id, created_at, id)` — 회원별 최신순 조회에 사용. ✅

### 세션 데이터 확장

세션에 저장되는 `SessionMember`에 역할(`role`)이 추가되었다(회원 ID, 이메일, 이름, 역할). 단, 접근 제어의 권한 판단은 세션 값이 아닌 **최신 DB 상태**로 수행한다. ✅

### 검증·처리 규칙 요약

| 번호 | 규칙 | 확인 상태 |
|------|------|-----------|
| AR-01 | 신규 가입자는 `VIEWER`/`ACTIVE`로 시작 | ✅ |
| AR-04 | 본인 역할 강등·본인 정지 금지 | ✅ |
| AR-05 | 활성 `ADMIN` 최소 1명 보장(단일 SQL 가드) | ✅ |
| AR-06 | 낙관적 잠금 충돌 시 409 | ✅ |
| AR-07 | 변경과 감사 로그를 동일 트랜잭션으로 기록 | ✅ |
| AR-09 | 사유 4~500자, 검색어 100자 이하 서버 검증 | ✅ |
| AR-10 | 휴대폰 번호·요청 IP 마스킹 | ✅ |

---

## Error Handling

관리자 API 오류는 `AccessManagementApiExceptionHandler`(`@RestControllerAdvice`, 대상 컨트롤러 한정)가 일관된 JSON으로 매핑한다.

| 오류 상황 | HTTP 상태 | 응답 |
|-----------|-----------|------|
| 미인증 요청(`/api/admin/**`) | 401 | `{"message":"로그인이 필요합니다."}` |
| 비관리자 요청 | 403 | `{"message":"접근 권한이 없습니다."}` |
| 입력값 오류(사유 길이·필수값 등) | 400 | `{"status":400,"message":"..."}` |
| 권한 규칙 위반(본인 강등/정지, 마지막 관리자) | 400 | `{"status":400,"message":"..."}` |
| 동시 수정 충돌(version 불일치) | 409 | `{"status":409,"message":"다른 관리자가 먼저 변경했습니다..."}` |
| 존재하지 않는 회원 | 404 | `{"status":404,"message":"대상 회원을 찾을 수 없습니다."}` |
| 예상하지 못한 오류 | 500 | `{"status":500,"message":"요청을 처리하지 못했습니다..."}` |
| CSRF 토큰 불일치(상태 변경) | 403 | 기존 CSRF 필터 응답 |

- 오류 응답에는 스택 트레이스·내부 상세 메시지를 노출하지 않는다. ✅
- 도메인 예외: `AccessRuleViolationException`(400), `MemberNotFoundException`(404), `OptimisticLockConflictException`(409), `SuspendedMemberException`(로그인 거부).

---

## Correctness Properties

### Property 1: 신규 회원 기본 권한

*For any* 신규 가입 회원에 대해, 저장된 역할은 항상 `VIEWER`, 상태는 항상 `ACTIVE`, `version`은 0이어야 한다.

**Validates: Requirements 1.2, 1.3**

---

### Property 2: 낙관적 잠금 단조 증가

*For any* 성공한 권한 변경에 대해, 변경 후 `version`은 변경 전 `version`보다 정확히 1 커야 하며, `expectedVersion`이 저장된 `version`과 다른 요청은 항상 거부(409)되어야 한다.

**Validates: Requirements 6.6, 8.1**

---

### Property 3: 변경–감사 로그 원자성

*For any* 권한 변경 시도에 대해, 회원 변경과 감사 로그 기록은 함께 성공하거나 함께 실패해야 한다. 즉, 감사 로그만 남거나 회원 정보만 바뀌는 중간 상태가 존재해서는 안 된다.

**Validates: Requirements 11.1, 11.3**

---

### Property 4: 활성 관리자 최소 1명 불변식

*For any* 권한 변경 실행 순서(동시 실행 포함)에 대해, 실행 후 시스템의 활성(`ACTIVE`) `ADMIN` 수는 항상 1명 이상이어야 한다.

**Validates: Requirements 7.3, 7.4**

---

### Property 5: 자기 권한 보호

*For any* 관리자에 대해, 자기 자신을 대상으로 역할을 `ADMIN` 이외로 낮추거나 상태를 `SUSPENDED`로 바꾸는 요청은 항상 거부(400)되어야 한다.

**Validates: Requirements 7.1, 7.2**

---

### Property 6: 정지 회원 접근 차단

*For any* 상태가 `SUSPENDED`인 회원에 대해, 로그인 시도는 거부되어야 하고, 기존 세션으로의 후속 요청은 화면 경로에서 로그인 이동, API 경로에서 401로 차단되어야 한다.

**Validates: Requirements 9.1, 9.3, 9.4**

---

### Property 7: 권한 기반 API 접근

*For any* `/api/admin/**` 요청에 대해, 미인증이면 401, 인증되었으나 `ADMIN`이 아니면 403, `ADMIN`이면 통과해야 한다.

**Validates: Requirements 10.1, 10.2**

---

### Property 8: 페이지 크기 상한

*For any* 목록/감사 로그 조회 요청의 `size` 값에 대해, 실제 적용되는 페이지 크기는 항상 1 이상 100 이하여야 하며, 0 이하 입력은 기본값으로 대체되어야 한다.

**Validates: Requirements 5.5**

---

### Property 9: 휴대폰 번호 마스킹

*For any* 유효한 11자리 휴대폰 번호에 대해, 목록 응답의 표시값은 항상 `010-****-XXXX` 형식이어야 하며 끝 4자리는 보존되어야 한다.

**Validates: Requirements 5.8**

---

### Property 10: 감사 로그 최신순 정렬

*For any* 회원의 감사 로그 조회 결과에 대해, 항목은 항상 변경 일시 내림차순(동일 시각은 ID 내림차순)으로 정렬되어야 한다.

**Validates: Requirements 11.4**

---

## Testing Strategy

구현과 함께 추가된 테스트(전체 스위트 통과)를 기준으로 한다.

### 단위 테스트 (Unit Tests) — `AccessManagementServiceTest`

| 검증 항목 |
|-----------|
| 역할·상태 변경 성공 및 감사 로그 기록(동일 트랜잭션) |
| 변경 사유 누락/4자 미만 → 400 |
| 본인 역할 강등·본인 정지 거부 |
| 마지막 활성 관리자 강등 거부, 다른 활성 관리자가 있으면 허용 |
| 동시 강등 경쟁 상태에서 가드 업데이트가 최종 차단 |
| version 불일치 → 409, 존재하지 않는 회원 → 404 |
| 검색 페이징·필터 파라미터 전달, `size` 상한 클램프 |
| 감사 로그 최신순 조회 및 요청 IP 마스킹 |

### 통합 테스트 (Integration Tests) — `AccessManagementApiControllerTest`, `AccessManagementPageControllerTest`

| 검증 항목 |
|-----------|
| 미인증 API → 401, `VIEWER`/`OPERATOR` → 403, `ADMIN` → 성공 |
| 역할·상태 변경 성공 및 감사 로그 DB 기록 확인 |
| 사유 오류 400, 자기 강등 400, version 충돌 409, 없는 회원 404 |
| CSRF 토큰 미포함 시 403 |
| 검색·역할 필터·상태 필터·페이지네이션 |
| 감사 로그 최신순 응답, 요청 IP 마스킹 표시 |
| 정지 회원 로그인 후 기존 세션 차단(401) |
| `/admin/access` ADMIN 200, VIEWER 403, 미인증 로그인 리다이렉트 |

### 초기 관리자 정책 — `BootstrapAdminInitializerTest`

| 검증 항목 |
|-----------|
| `BOOTSTRAP_ADMIN_EMAIL` 회원을 ADMIN/ACTIVE로 승격 |
| 이메일 공백 제거·소문자 정규화 |
| 이미 ADMIN/ACTIVE면 변경 없음(멱등) |
| 빈 값이면 아무 작업 없음, 대상 회원 없으면 경고 후 진행 |

> **검증 환경:** H2(MySQL 호환 모드) 기반 `@SpringBootTest` + MockMvc. 로컬 MySQL 미기동으로 실제 MySQL 8 대상 실행은 미검증이며, 가드 업데이트 SQL은 표준 문법(파생 테이블 래핑)으로 작성했다. 🔶

---

## 보안 처리 방식

| 항목 | 적용 방식 |
|------|-----------|
| 권한 판단 기준 | 세션이 아닌 최신 DB 상태(역할·상태)로 판단 |
| 관리자 화면 보호 | `/admin/**` 비관리자 403, 미인증 로그인 이동 |
| 관리자 API 보호 | `/api/admin/**` 미인증 401, 비관리자 403 (JSON) |
| 정지 회원 차단 | 로그인 거부 + 기존 세션 다음 요청에서 무효화 |
| 위조 요청 방지(CSRF) | 상태 변경(PUT)에 세션 기반 CSRF 토큰 검증 |
| 동시성 제어 | 낙관적 잠금(version) + 마지막 관리자 단일 SQL 가드 |
| 개인정보 보호 | 휴대폰 번호·요청 IP 마스킹 |
| SQL Injection 방지 | MyBatis 파라미터 바인딩(`#{...}`) 및 LIKE 이스케이프 |
| 오류 정보 노출 방지 | 스택 트레이스·내부 메시지 미노출, 일관된 한국어 JSON |

---

## 문서 관계

| 문서 | 범위 |
|------|------|
| [`member-auth-baseline`](../member-auth-baseline/) | 권한 구분 이전의 초기 시스템 스냅샷(회원가입·로그인·회원 조회) |
| **본 스펙(`member-access-management`)** | 그 이후 추가된 역할·상태·권한 관리·감사 로그 기능 |
| `README.md`, `.kiro/steering/*` | 항상 최신 구현을 반영(권한 관리 포함) |
