# Requirements Document

## Introduction

이 문서는 **BSG Partners** 회원관리 웹 애플리케이션에 추가된 **권한 관리(Access Management)** 기능의 구현 기준 요구사항을 정리한 것입니다.

기존 [`member-auth-baseline`](../member-auth-baseline/requirements.md) 스펙은 권한 구분이 없던 초기 시스템의 스냅샷입니다. 본 스펙은 그 이후 추가된 권한 관리 기능을 **별도로** 다루며, 실제 구현 코드를 직접 확인하여 작성되었습니다.

> **분석 기준:** 현재 구현 코드 직접 확인
> **관련 커밋:** `feat: 관리자용 권한 관리 기능 추가`, `fix: 권한 관리 페이징 응답 오류 수정`, `refactor: DB 스키마를 Liquibase changeSet 기반으로 마이그레이션`, `fix: 권한 관리 그리드 깜빡임 개선 및 행 구분선 추가`
> **표기 규칙:**
> - ✅ 확인됨 — 실제 구현 코드에서 직접 확인한 사실
> - 🔶 추정 — 구현을 근거로 판단했으나 담당자 확인 권장
> - ❓ 확인 필요 — 코드만으로는 정확히 알 수 없는 내용

이 기능은 기존 `member-auth-baseline` 스펙의 확인 항목 **Q1**("로그인한 모든 회원이 전체 목록을 조회할 수 있는데, 일반 회원과 관리자를 구분해야 하는지")에 대한 응답으로 도입되었습니다. 이제 회원은 역할(`ADMIN`/`OPERATOR`/`VIEWER`)과 계정 상태(`ACTIVE`/`SUSPENDED`)를 가지며, `ADMIN`만 다른 회원의 권한을 변경할 수 있습니다.

---

## Glossary

- **역할(Role)**: 회원의 권한 등급. `ADMIN`(관리자) / `OPERATOR`(운영자) / `VIEWER`(조회자) 세 가지
- **계정 상태(Status)**: 계정의 활성화 여부. `ACTIVE`(활성) / `SUSPENDED`(정지)
- **관리자(ADMIN)**: 권한 관리 화면과 API를 사용해 다른 회원의 역할·상태를 변경할 수 있는 회원
- **낙관적 잠금(Optimistic Lock)**: 데이터에 `version` 번호를 두고, 조회 시점의 `version`과 저장 시점의 `version`이 다르면 충돌로 간주하는 동시성 제어 방식
- **감사 로그(Audit Log)**: 역할·상태 변경 이력을 남기는 기록. 변경 대상·수행자·변경 전후 값·사유·요청 IP·일시를 포함
- **초기 관리자(Bootstrap Admin)**: 애플리케이션 시작 시 환경변수로 지정해 `ADMIN`/`ACTIVE`로 승격하는 회원
- **actor(수행자)**: 권한 변경을 요청한, 현재 로그인한 관리자
- **target(대상)**: 권한 변경의 대상이 되는 회원

---

## Requirements

### Requirement 1: 역할 및 계정 상태 도입

**User Story:** As a 시스템, I want 회원마다 역할과 계정 상태를 부여할 수 있기를, so that 권한 수준에 따라 기능 접근을 통제할 수 있다.

#### Acceptance Criteria

1. THE System SHALL 회원에게 역할(`ADMIN`/`OPERATOR`/`VIEWER`)과 계정 상태(`ACTIVE`/`SUSPENDED`)를 부여한다. ✅
2. WHEN 신규 회원이 가입하면, THE System SHALL 역할을 `VIEWER`, 계정 상태를 `ACTIVE`로 기본 설정한다. ✅
3. THE System SHALL 각 회원에 낙관적 잠금용 `version`(기본값 0)과 권한 최종 변경 일시(`role_updated_at`)를 보관한다. ✅
4. THE System SHALL 기존 회원 데이터를 삭제·초기화하지 않고 안전하게 스키마를 확장하며, 기존 회원은 기본값(`VIEWER`/`ACTIVE`/`version=0`)으로 채운다. ✅

---

### Requirement 2: 초기 관리자 지정 (Bootstrap Admin)

**User Story:** As a 운영자, I want 운영 환경에서 최초 관리자를 지정할 수 있기를, so that 신규 가입자가 모두 `VIEWER`인 상태에서도 권한 관리를 시작할 수 있다.

#### Acceptance Criteria

1. WHERE 환경변수 `BOOTSTRAP_ADMIN_EMAIL`이 설정된 경우, WHEN 애플리케이션이 시작되면, THE System SHALL 해당 이메일 회원을 `ADMIN`/`ACTIVE`로 승격한다. ✅
2. THE System SHALL 이메일을 공백 제거 후 소문자로 정규화하여 대상 회원을 조회한다. ✅
3. IF 대상 회원이 이미 `ADMIN`/`ACTIVE`이면, THEN THE System SHALL 아무 변경도 하지 않는다(멱등성). ✅
4. IF `BOOTSTRAP_ADMIN_EMAIL`에 해당하는 회원이 없으면, THEN THE System SHALL 경고 로그만 남기고 애플리케이션 시작을 계속한다. ✅
5. WHERE `BOOTSTRAP_ADMIN_EMAIL`이 비어 있으면, THE System SHALL 아무 작업도 수행하지 않는다. ✅
6. WHERE 데모 시드가 활성(`SEED_DEMO_MEMBERS=true`)인 경우, THE System SHALL `member001@bsg-demo.local`을 `ADMIN`, `member002@bsg-demo.local`을 `OPERATOR`로 지정한다. ✅

---

### Requirement 3: 권한 관리 메뉴 및 화면 노출

**User Story:** As an 관리자, I want 좌측 사이드바에서 권한 관리 메뉴로 진입할 수 있기를, so that 회원의 역할과 상태를 관리할 수 있다.

#### Acceptance Criteria

1. THE System SHALL 로그인 후 좌측 사이드바를 `회원 관리`, `권한 관리` 두 최상위 메뉴로 구성한다. ✅
2. WHERE 현재 로그인 회원이 `ADMIN`인 경우에만, THE System SHALL 사이드바에 `권한 관리` 메뉴를 노출한다. ✅
3. THE System SHALL 회원 관리 화면에서는 `회원 관리` 메뉴를, 권한 관리 화면에서는 `권한 관리` 메뉴를 활성 상태로 표시한다. ✅
4. THE System SHALL 권한 관리 화면 경로를 `/admin/access`로 제공한다. ✅
5. THE System SHALL 공통 헤더와 사이드바를 Thymeleaf fragment(`fragments/layout`)로 공통화한다. ✅
6. THE System SHALL 기존 BSG Partners 화면 디자인과 반응형 스타일을 유지한다. ✅

---

### Requirement 4: 권한 관리 화면 접근 제어

**User Story:** As a 시스템, I want 관리자가 아닌 사용자의 권한 관리 화면 접근을 차단할 수 있기를, so that 권한 변경 기능을 보호할 수 있다.

#### Acceptance Criteria

1. WHEN 미인증 사용자가 `/admin/access`에 접근하면, THE System SHALL 로그인 화면으로 이동시킨다. ✅
2. WHEN 인증되었으나 `ADMIN`이 아닌 사용자가 `/admin/access`에 접근하면, THE System SHALL HTTP 403 오류 화면을 표시한다. ✅
3. THE System SHALL 접근 권한을 세션 정보가 아닌 최신 DB 상태(역할·상태)로 판단한다. ✅
4. WHEN `SUSPENDED`로 변경된 회원이 기존 세션으로 접근하면, THE System SHALL 세션을 무효화하고 로그인 화면으로 이동시킨다. ✅

---

### Requirement 5: 권한 관리 대상 회원 목록 조회 (API/화면)

**User Story:** As an 관리자, I want 회원을 검색·필터·페이지 단위로 조회할 수 있기를, so that 대상 회원을 찾아 권한을 변경할 수 있다.

#### Acceptance Criteria

1. WHEN 관리자가 권한 관리 화면에 진입하면, THE System SHALL AJAX로 `GET /api/admin/members`를 호출해 목록을 표시한다. ✅
2. THE System SHALL 이름, 이메일, 휴대폰 번호를 대상으로 대소문자 구분 없이 키워드 부분 일치 검색을 제공한다. ✅
3. THE System SHALL 역할 필터(`ADMIN`/`OPERATOR`/`VIEWER`)와 계정 상태 필터(`ACTIVE`/`SUSPENDED`)를 제공한다. ✅
4. THE System SHALL 서버 사이드 페이지네이션을 제공하며 기본 페이지 크기를 20으로 한다. ✅
5. THE System SHALL `size` 파라미터에 최댓값 100을 적용하고, 0 이하이면 기본값 20으로 처리한다. ✅
6. THE System SHALL 목록에 번호, 이름, 이메일, 휴대폰 번호(마스킹), 역할, 상태, 변경 일시를 표시하고 전체 건수(총 N명)를 함께 제공한다. ✅
7. THE System SHALL 목록을 ID 오름차순으로 정렬한다. ✅
8. THE System SHALL 휴대폰 번호를 조회 시 중간 4자리를 숨김 처리하여 표시하며(예: `010-****-5678`), 미등록 시 "미등록"을 표시한다. ✅
9. IF 검색어 길이가 100자를 초과하면, THEN THE System SHALL HTTP 400 오류를 반환한다. ✅

---

### Requirement 6: 회원 역할 및 상태 변경 (API/화면)

**User Story:** As an 관리자, I want 회원 한 명의 역할과 상태를 변경 사유와 함께 변경할 수 있기를, so that 조직의 권한 정책을 반영할 수 있다.

#### Acceptance Criteria

1. THE System SHALL `PUT /api/admin/members/{id}/access`로 대상 회원의 역할·상태 변경을 제공한다. ✅
2. THE System SHALL 변경 요청에 역할, 상태, 변경 사유, 조회 시점의 `version`(`expectedVersion`)을 포함하도록 요구한다. ✅
3. THE System SHALL 변경 사유를 4자 이상 500자 이하로 검증하며, 누락 또는 범위를 벗어나면 HTTP 400을 반환한다. ✅
4. THE System SHALL 저장 전 변경 내용을 확인하는 UI(확인 다이얼로그)를 제공한다. ✅
5. WHEN 변경이 성공하면, THE System SHALL 변경 결과(갱신된 역할·상태·`version`)를 반환하고 화면에 성공 메시지를 표시한다. ✅
6. WHEN 변경이 성공하면, THE System SHALL 대상 회원의 `version`을 1 증가시키고 권한 최종 변경 일시를 한국 표준시 기준으로 갱신한다. ✅
7. THE System SHALL 모든 규칙을 클라이언트가 아닌 서버에서 다시 검증한다. ✅

---

### Requirement 7: 권한 변경 비즈니스 규칙

**User Story:** As a 시스템, I want 위험한 권한 변경을 서버에서 차단할 수 있기를, so that 시스템이 관리 불능 상태에 빠지지 않도록 한다.

#### Acceptance Criteria

1. IF 현재 로그인한 관리자가 본인의 역할을 `ADMIN`이 아닌 값으로 변경하려 하면, THEN THE System SHALL HTTP 400 오류("본인의 관리자 역할을 낮출 수 없습니다.")를 반환한다. ✅
2. IF 현재 로그인한 관리자가 본인 계정을 `SUSPENDED`로 변경하려 하면, THEN THE System SHALL HTTP 400 오류("본인 계정을 정지할 수 없습니다.")를 반환한다. ✅
3. IF 마지막 남은 활성 `ADMIN`을 강등하거나 정지하려 하면, THEN THE System SHALL HTTP 400 오류("활성 상태의 관리자가 최소 1명 이상 남아 있어야 합니다.")를 반환한다. ✅
4. THE System SHALL 마지막 활성 관리자 보호를 단일 SQL 가드 업데이트로 강제하여, 두 관리자의 동시 강등으로 활성 관리자가 0명이 되는 경쟁 상태를 방지한다. ✅
5. IF 존재하지 않는 회원 ID로 변경을 요청하면, THEN THE System SHALL HTTP 404 오류를 반환한다. ✅

---

### Requirement 8: 동시 수정 충돌 처리 (낙관적 잠금)

**User Story:** As a 시스템, I want 두 관리자가 같은 회원을 동시에 변경하는 상황을 감지할 수 있기를, so that 나중 저장이 먼저 저장을 덮어쓰지 않도록 한다.

#### Acceptance Criteria

1. IF 변경 요청의 `expectedVersion`이 저장된 `version`과 다르면, THEN THE System SHALL HTTP 409 오류를 반환한다. ✅
2. WHEN HTTP 409 충돌이 발생하면, THE System SHALL 감사 로그를 기록하지 않는다(변경 자체가 이루어지지 않으므로). ✅
3. WHEN 화면에서 HTTP 409 충돌이 발생하면, THE System SHALL 최신 데이터로 목록을 다시 조회하도록 안내한다. ✅

---

### Requirement 9: 정지 회원 로그인 및 세션 차단

**User Story:** As a 시스템, I want 정지된 회원의 접근을 즉시 차단할 수 있기를, so that 권한 회수가 지연 없이 반영된다.

#### Acceptance Criteria

1. IF `SUSPENDED` 상태의 회원이 올바른 자격 증명으로 로그인을 시도하면, THEN THE System SHALL 로그인을 거부하고 오류 메시지를 표시한다. ✅
2. THE System SHALL 정지 여부 검사를 비밀번호 검증 이후에 수행하여, 자격 증명을 제시한 요청에만 정지 사유를 노출한다. ✅
3. WHEN 로그인 이후 회원이 `SUSPENDED`로 변경되면, THE System SHALL 해당 회원의 기존 세션을 다음 요청에서 차단하고 세션을 무효화한다. ✅
4. WHEN 정지된 회원이 화면 경로에 접근하면 로그인 화면으로 이동시키고, API 경로에 접근하면 HTTP 401을 반환한다. ✅

---

### Requirement 10: 관리자 API 접근 제어 및 오류 응답

**User Story:** As a 시스템, I want 관리자 API를 인증·권한·CSRF로 보호하고 일관된 오류 형식을 제공할 수 있기를, so that 안전하고 예측 가능한 API를 유지한다.

#### Acceptance Criteria

1. WHEN 미인증 요청이 `/api/admin/**`에 들어오면, THE System SHALL HTTP 401 JSON을 반환한다. ✅
2. WHEN 인증되었으나 `ADMIN`이 아닌 요청이 `/api/admin/**`에 들어오면, THE System SHALL HTTP 403 JSON을 반환한다. ✅
3. THE System SHALL 모든 상태 변경 요청(예: `PUT /api/admin/members/{id}/access`)에 기존 CSRF 토큰 정책을 적용하며, 토큰 불일치 시 HTTP 403을 반환한다. ✅
4. THE System SHALL 관리자 API 오류를 `{ "status": <숫자>, "message": "<한국어 메시지>" }` 형식으로 일관되게 반환한다. ✅
5. THE System SHALL 오류 응답에 스택 트레이스나 내부 상세 메시지를 노출하지 않으며, 예상하지 못한 오류에는 HTTP 500과 일반 안내 메시지를 반환한다. ✅
6. THE System SHALL 모든 DB 접근에 MyBatis 파라미터 바인딩을 사용하여 SQL Injection을 방지한다. ✅

---

### Requirement 11: 권한 변경 감사 로그

**User Story:** As an 관리자, I want 모든 권한·상태 변경 이력을 조회할 수 있기를, so that 누가 언제 무엇을 왜 바꿨는지 추적할 수 있다.

#### Acceptance Criteria

1. WHEN 역할 또는 상태 변경이 성공하면, THE System SHALL 감사 로그를 1건 기록한다. ✅
2. THE System SHALL 감사 로그에 대상 회원 ID, 수행자 회원 ID, 변경 전 역할·상태, 변경 후 역할·상태, 변경 사유, 변경 일시, 요청 IP를 포함한다. ✅
3. THE System SHALL 회원 권한 변경과 감사 로그 저장을 동일한 트랜잭션에서 처리하여, 변경 실패 시 감사 로그만 남거나 회원 정보만 변경되는 일이 없도록 한다. ✅
4. THE System SHALL `GET /api/admin/members/{id}/audit-logs`로 특정 회원의 감사 로그를 최신순으로 서버 페이지네이션하여 제공한다(기본 페이지 크기 10, 권한 관리 화면도 동일하게 10건씩 요청). ✅
5. THE System SHALL 권한 관리 화면에서 선택한 회원의 최근 감사 로그를 모달 상세 영역에 표시한다. ✅
6. THE System SHALL 감사 로그 조회 시 요청 IP를 마스킹하여 표시한다(예: `10.0.0.***`). ✅

---

## 업무 규칙 요약

| 번호 | 규칙 | 확인 상태 |
|------|------|-----------|
| AR-01 | 신규 가입자는 역할 `VIEWER`, 상태 `ACTIVE`로 시작한다 | ✅ |
| AR-02 | `ADMIN`만 다른 회원의 역할·상태를 변경할 수 있으며, `OPERATOR`/`VIEWER`는 권한 관리 메뉴·API를 사용할 수 없다 | ✅ |
| AR-03 | `SUSPENDED` 회원은 로그인할 수 없고, 로그인 이후 정지되면 다음 요청에서 세션이 차단·무효화된다 | ✅ |
| AR-04 | 현재 로그인한 관리자는 자기 자신의 역할을 낮추거나 계정을 정지할 수 없다 | ✅ |
| AR-05 | 시스템에는 활성 상태의 `ADMIN`이 최소 1명 이상 남아 있어야 한다 | ✅ |
| AR-06 | 동시 수정은 `version`(낙관적 잠금)으로 감지하여 충돌 시 HTTP 409를 반환한다 | ✅ |
| AR-07 | 모든 권한/상태 변경은 변경과 동일 트랜잭션에서 감사 로그로 기록한다 | ✅ |
| AR-08 | 접근 권한은 세션이 아닌 최신 DB 상태로 판단한다 | ✅ |
| AR-09 | 변경 사유는 4자 이상 500자 이하, 검색어는 100자 이하로 서버에서 검증한다 | ✅ |
| AR-10 | 휴대폰 번호는 조회 시 마스킹하고, 감사 로그의 요청 IP도 마스킹하여 표시한다 | ✅ |
| AR-11 | 초기 관리자는 `BOOTSTRAP_ADMIN_EMAIL`로 지정하며 시작 시 멱등하게 승격한다 | ✅ |

---

## 확인이 필요한 사항

| 번호 | 확인 항목 | 이유 |
|------|-----------|------|
| Q1 | 감사 로그의 보존 기간 및 폐기 정책이 필요한가? | 현재는 무기한 보관하며 삭제 기능이 없다 |
| Q2 | 요청 IP 마스킹 수준(마지막 옥텟 숨김)이 감사 목적에 충분한가? | 감사 추적 요구와 개인정보 보호 사이의 균형 확인 필요 |
| Q3 | 감사 로그 조회 페이지 크기(API 기본 10, 화면 5)를 통일할 필요가 있는가? | 현재 API 기본값과 화면 요청값이 다르다 |
| Q4 | 역할 종류(`ADMIN`/`OPERATOR`/`VIEWER`)별로 향후 차등 권한(예: `OPERATOR` 전용 기능)이 필요한가? | 현재 `OPERATOR`와 `VIEWER`는 권한 관리 접근 측면에서 동일하게 취급된다 |

---

## 현재 구현되지 않은 기능 (개선 검토 후보)

- 역할·상태 일괄(다중 회원) 변경
- 감사 로그 전체(회원 무관) 통합 조회 화면
- 감사 로그 CSV/엑셀 내보내기
- 역할별 세분화된 기능 권한(현재 `OPERATOR`/`VIEWER`는 권한 관리 접근 불가라는 점에서 동일)
- 권한 변경 승인(2인 결재) 워크플로
