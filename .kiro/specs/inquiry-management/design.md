# 문의 요청(Inquiry) 설계 문서

> **문서 목적:** 문의 요청 기능의 구현 기준 업무 흐름 및 구조 설명 (다른 스펙과 분리 관리)
> **표기 규칙:**
> - ✅ 확인됨 — 실제 구현 코드에서 직접 확인한 사실
> - 🔶 추정 — 구현을 근거로 판단했으나 담당자 확인 권장
> - ❓ 확인 필요

---

## Overview

**문의 요청 기능**은 로그인한 회원이 제목·내용으로 문의를 등록하고, 전체 문의를 최신순 페이지 단위로 조회하며, 개별 문의 상세를 열람(조회수 증가)하는 게시글형 기능이다. 기존 인증·세션 인프라 위에서 동작하며, 좌측 사이드바 최상위 메뉴 `문의 요청`(`/inquiries`)으로 진입한다.

| 사용 대상 | 문의 기능 접근 |
|-----------|----------------|
| 미로그인 방문자 | 접근 불가 → 로그인 화면 |
| 로그인 회원(역할 무관) | 목록·작성·상세 조회 가능 |

**기술 스택:** 기존 시스템과 동일(Spring Boot, Thymeleaf, MyBatis, MySQL). DB 스키마는 **Liquibase** changeSet(`8-create-inquiries`)으로 관리한다.

---

## Architecture

기존 시스템의 **헥사고날 아키텍처**를 따르며, 쓰기(작성)와 읽기(목록/상세)를 CQRS로 분리한다.

```
[브라우저 / 로그인 회원]
        │  /inquiries (목록), /inquiries/new (작성 폼), POST /inquiries, /inquiries/{id} (상세)
        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Adapter (in / web)                            │
│  InquiryController — 목록/작성 폼/작성/상세                     │
│  AuthenticationInterceptor — /inquiries/** 인증(미인증 로그인 이동)│
│  CsrfTokenFilter — 작성(POST) CSRF 검증                         │
└─────────────────────────────────────────────────────────────┘
        │  port/in (Use Case)
        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Application (service)                         │
│  InquiryService       — CreateInquiryUseCase (작성)            │
│  InquiryQueryService  — InquiryQueryUseCase (목록/상세+조회수) │
└─────────────────────────────────────────────────────────────┘
        │  port/out (Repository)
        ▼
┌─────────────────────────────────────────────────────────────┐
│                  Adapter (out / persistence)                   │
│  MyBatisInquiryRepository      — insert, 조회수 증가, 단건 조회 │
│  MyBatisInquiryQueryRepository — 목록 페이지 조회, 전체 카운트  │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│                       Database (MySQL)                         │
│  inquiries (member_id FK → members.id)                         │
│  — Liquibase changeSet 8-create-inquiries                      │
└─────────────────────────────────────────────────────────────┘
```

> **의존성 규칙:** 기존 프로젝트 규칙과 동일. 애플리케이션 서비스에는 Spring 어노테이션이 없으며 `InquiryConfiguration`이 빈을 수동 등록한다.

### 주요 업무 흐름

#### 2.1 문의 목록 조회

```
[사이드바] "문의 요청" 클릭 → GET /inquiries?page=1
  │
  ├─ (미인증 → 로그인 화면)
  │
  ├─ 전체 건수 조회 → 전체 페이지 수 계산(페이지당 10건, 최소 1)
  ├─ 요청 page 를 [1, totalPages] 로 보정
  ├─ 최신순 목록 조회(작성일시 DESC, ID DESC)
  └─ 목록 표시: 제목 / 조회수 / 작성일시 + 이전·다음 페이지
```

#### 2.2 문의 작성

```
[문의 목록] "작성" → GET /inquiries/new (폼)
  │
  ├─ 제목(≤20자, 필수) / 내용(≤500자, 필수) 입력 → POST /inquiries (CSRF)
  │
  ├─ 유효성 실패 → 폼 유지, 오류 메시지 표시
  └─ 성공 → 작성자=현재 로그인 회원, 조회수=0, 작성일시=Asia/Seoul
            → redirect: /inquiries/{생성 id}
```

#### 2.3 문의 상세 조회

```
GET /inquiries/{id}
  │
  ├─ 문의 단건 조회(작성자 이름은 members 조인)
  ├─ 없음 → HTTP 404
  ├─ 조회수 +1 (DB 업데이트)
  └─ 상세 표시(조회수는 +1 반영된 값)
```

---

## Components and Interfaces

### 화면 경로 (URL Endpoints)

| 경로 | 메서드 | 로그인 필요 | 설명 |
|------|--------|------------|------|
| `/inquiries` | GET | 필요 | 문의 목록(페이지네이션, 기본 page=1, size 10) |
| `/inquiries/new` | GET | 필요 | 문의 작성 폼 |
| `/inquiries` | POST | 필요 | 문의 등록(CSRF), 성공 시 상세로 리다이렉트 |
| `/inquiries/{id}` | GET | 필요 | 문의 상세(조회수 +1), 없으면 404 |

### 주요 파일 위치

| 역할 | 경로 |
|------|------|
| 문의 컨트롤러 | `adapter/in/web/InquiryController.java` |
| 작성 폼 DTO(검증) | `adapter/in/web/form/InquiryForm.java` |
| 작성 업무 규칙 | `application/service/InquiryService.java` |
| 목록·상세 업무 규칙 | `application/service/InquiryQueryService.java` |
| 도메인 | `domain/inquiry/Inquiry.java` |
| 쓰기 쿼리 | `resources/mapper/InquiryMapper.xml` |
| 읽기 쿼리 | `resources/mapper/InquiryQueryMapper.xml` |
| 빈 등록 | `config/InquiryConfiguration.java` |
| 인증 인터셉터 등록 | `config/WebConfiguration.java` (`/inquiries/**` 포함) |
| DB 스키마 이력 | `resources/db/changelog/db.changelog-master.yaml` (`8-create-inquiries`) |
| 화면 템플릿 | `resources/templates/inquiry/{list,form,detail}.html` |

### 화면 구성

기존 회원 관리 화면과 동일한 상단 헤더 + 좌측 사이드바(공통 fragment) 레이아웃을 사용한다.

- **목록(`inquiry/list.html`):** 헤딩, 문의 목록(제목/조회수/작성일시), 이전·다음 페이지, `문의 요청` 메뉴 활성
- **작성(`inquiry/form.html`):** 제목 입력, 내용 입력, 필드별 오류 메시지, 등록 버튼(CSRF 토큰 포함)
- **상세(`inquiry/detail.html`):** 제목, 작성자, 작성일시, 조회수, 내용

---

## Data Models

### 문의 (`inquiries` 테이블)

Liquibase changeSet `8-create-inquiries`가 테이블 존재 여부를 precondition으로 감지해 생성한다(기존 테이블·데이터는 유지).

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT AUTO_INCREMENT | PK | 문의 식별 번호 |
| `member_id` | BIGINT | NOT NULL, FK → `members(id)` (`fk_inquiries_member`) | 작성자 회원 |
| `title` | VARCHAR(20) | NOT NULL | 제목(최대 20자) |
| `content` | TEXT | NOT NULL | 내용(폼에서 최대 500자 검증) |
| `view_count` | BIGINT | NOT NULL, 기본값 0 | 조회수 |
| `created_at` | DATETIME(6) | NOT NULL | 작성일시(Asia/Seoul) |

> 🔶 `content`는 DB상 `TEXT`이지만, 작성 폼(`InquiryForm`)에서 500자 이하로 검증한다. 직접 DB 삽입 시에는 더 긴 값이 저장될 수 있다.

### 도메인/DTO

- `Inquiry(id, memberId, title, content, viewCount, createdAt)` — 도메인 record
- `InquirySummary(id, title, viewCount, createdAt)` — 목록 항목
- `InquiryDetail(id, authorName, title, content, viewCount, createdAt)` — 상세
- `InquiryPage(items, currentPage, totalPages, totalCount)` — `hasPrevious()`/`hasNext()` 제공
- `InquiryQueryResult(id, authorName, title, content, viewCount, createdAt)` — 조회 결과(작성자 이름 조인)

### 처리 규칙 요약

| 번호 | 규칙 | 확인 상태 |
|------|------|-----------|
| IR-04 | 신규 문의 조회수 0에서 시작 | ✅ |
| IR-05 | 상세 열람 시 조회수 +1 | ✅ |
| IR-06 | 목록 최신순, 페이지당 10건 | ✅ |
| IR-07 | 작성일시 Asia/Seoul 저장 | ✅ |
| IR-08 | 작성자 = 세션의 현재 로그인 회원 | ✅ |

---

## Error Handling

| 오류 상황 | 응답 방식 | 내용 |
|-----------|-----------|------|
| 미인증 사용자 → `/inquiries/**` 접근 | HTTP 302 리다이렉트 | `/login`으로 이동 |
| 존재하지 않는 문의 상세 조회 | HTTP 404 | `ResponseStatusException(NOT_FOUND)` (`InquiryNotFoundException`) |
| 작성 폼 입력값 검사 실패 | 동일 화면 유지 | 필드별 오류 메시지("제목을 입력해 주세요." 등) |
| 작성(POST) CSRF 토큰 불일치 | HTTP 403 오류 화면 | 기존 CSRF 필터 응답 |

---

## Correctness Properties

### Property 1: 페이지 번호 보정

*For any* 요청 페이지 번호에 대해, 실제 사용되는 페이지는 항상 1 이상 전체 페이지 수 이하여야 한다.

**Validates: Requirements 1.3**

---

### Property 2: 조회수 단조 증가

*For any* 문의 상세 조회 요청에 대해, 조회 후 저장된 조회수는 조회 전보다 정확히 1 커야 하며, 상세 화면에 표시되는 조회수는 증가가 반영된 값이어야 한다.

**Validates: Requirements 3.2, 3.3**

---

### Property 3: 작성 필드 검증

*For any* 문의 작성 입력에 대해, 제목이 비어 있거나 20자를 초과하거나 내용이 비어 있거나 500자를 초과하면 등록이 거부되어야 한다.

**Validates: Requirements 2.2, 2.3**

---

### Property 4: 최신순 정렬

*For any* 문의 목록 조회 결과에 대해, 항목은 항상 작성일시 내림차순(동일 시각은 ID 내림차순)으로 정렬되어야 한다.

**Validates: Requirements 1.1**

---

### Property 5: 신규 문의 초기 상태

*For any* 신규 등록 문의에 대해, 저장 직후 조회수는 0이고 작성자는 요청 회원, 작성일시는 서버 시계(Asia/Seoul) 기준이어야 한다.

**Validates: Requirements 2.5, 2.6**

---

## Testing Strategy

구현과 함께 추가된 테스트를 기준으로 한다.

### 단위 테스트

| 대상 | 검증 항목 |
|------|-----------|
| `InquiryServiceTest` | 작성 시 회원 ID·제목·내용·조회수 0·작성일시 저장 |
| `InquiryFormTest` | 제목/내용 필수 및 길이(20/500자) 검증 |

### 컨트롤러 테스트 — `InquiryControllerTest`

| 검증 항목 |
|-----------|
| 목록 화면(`inquiry/list`) 반환 및 모델 바인딩 |
| 작성 폼(`inquiry/form`) 반환 |
| 상세(`inquiry/detail`) 반환 및 모델 바인딩 |
| 존재하지 않는 문의 상세 → 404 |

### 마이그레이션 테스트 — `InquirySchemaMigrationTest`

| 검증 항목 |
|-----------|
| Liquibase가 `inquiries` 스키마를 생성하고 재실행해도 안전(멱등) |
| 기존 `inquiries` 테이블·데이터가 있으면 유지하고 changeSet은 MARK_RAN 처리 |
| `member_id` FK 제약 동작(잘못된 회원 참조 삽입 실패) |

> **검증 환경:** H2(MySQL 호환 모드) 기반 테스트. 실제 MySQL 8 대상 실행은 로컬 DB 미기동으로 미검증. 🔶

---

## 보안 처리 방식

| 항목 | 적용 방식 |
|------|-----------|
| 미인증 접근 차단 | `/inquiries/**`는 `AuthenticationInterceptor`로 로그인 필수 |
| 위조 요청 방지(CSRF) | 문의 등록(POST)에 세션 기반 CSRF 토큰 검증 |
| 작성자 위조 방지 | 작성자를 폼 입력이 아닌 세션의 현재 로그인 회원으로 설정 |
| SQL Injection 방지 | MyBatis 파라미터 바인딩(`#{...}`) 사용 |

---

## 문서 관계

| 문서 | 범위 |
|------|------|
| [`member-auth-baseline`](../member-auth-baseline/) | 권한 구분 이전 초기 시스템 스냅샷 |
| [`member-access-management`](../member-access-management/) | 역할·상태·권한 관리·감사 로그 |
| **본 스펙(`inquiry-management`)** | 문의 작성·목록·상세(조회수) 기능 |
| `README.md`, `.kiro/steering/*` | 항상 최신 구현 반영(문의 요청 포함) |
