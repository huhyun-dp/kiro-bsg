# Requirements Document

> **Spec:** inquiry-management · **종류:** living spec

## Introduction

이 문서는 **BSG Partners** 회원관리 웹 애플리케이션의 문의 요청(Inquiry) 기능 기준이다. 로그인한 회원은 문의를 작성·목록 조회·상세 조회할 수 있고, PDF/PNG/JPEG 첨부파일을 비공개 저장소에 등록·다운로드할 수 있다.

작성자 본인의 문의 편집이 구현되어 있다. 편집 화면에서 기존 첨부는 기본 보존되며, 개별 삭제는 별도의 CSRF 보호 요청(`POST /inquiries/{id}/attachments/{attachmentId}/delete`)으로 **즉시** 수행한다. 제목·내용 수정과 새 파일 추가는 편집 POST(`POST /inquiries/{id}/edit`)로 처리한다. 최종 첨부는 최대 5개·20 MiB, 새 파일 하나는 최대 10 MiB다.

> **개별 삭제 방식 변경(이전 설계 대체):** 이전에는 편집 multipart 요청에 `deleteAttachmentIds`를 실어 제목·내용 수정·새 파일 추가와 함께 지연(deferred) 삭제했다. 본 스펙은 이를 **편집 화면 내 즉시(per-attachment) 삭제 엔드포인트**로 대체한다. `deleteAttachmentIds` 기반 지연 삭제 서술은 더 이상 유효하지 않으며, 아래 요구사항이 최종 기준이다.

- ✅ 구현됨(이 문서 갱신 시 코드 정적 검토 기준)
- ⚠ 구현됐으나 9.7.8·9.7.9가 사용자 요청으로 건너뛰어 자동 검증되지 않음
- 🔶 예정된 구현 작업
- ❓ 제품 결정 필요(구현 작업 아님)

> **검증 한계:** 9.7.8(단위/속성 기반 테스트) 및 9.7.9(controller·integration·migration 회귀 테스트)는 사용자 요청으로 건너뛰었다. 본 문서화 작업은 테스트, 빌드, 또는 마이그레이션을 실행하지 않았으며, 아래 편집 확장 항목의 자동 검증 또는 빌드 통과를 의미하지 않는다.

## Glossary

- **문의(Inquiry)**: 로그인한 회원이 작성하는 제목·내용 기반 게시글
- **작성자(Author)**: 문의를 등록한 회원. 편집 및 첨부 삭제는 작성자만 가능하다.
- **첨부파일(Attachment)**: 서버 검증을 통과해 비공개 저장소와 메타데이터에 보관되는 파일
- **기존 첨부(Existing attachment)**: 편집 전 이미 문의에 연결된 첨부. 명시적으로 삭제하지 않으면 보존된다.
- **신규 첨부(New attachment)**: 편집 또는 작성 요청으로 추가되는 파일
- **표시 파일명(Display filename)**: 사용자에게만 표시·다운로드 제안하는 NFC 정규화 원본 파일명. 경로나 저장 키로 사용하지 않는다.
- **저장 키(Storage key)**: 서버가 생성한 UUID 기반 식별자. 비공개 실제 파일명 및 DB 메타데이터 연결에만 사용한다.

## Requirements

### Requirement 1: 문의 목록 조회

**User Story:** As an 인증된 회원, I want 등록된 문의를 페이지 단위로 조회할 수 있기를, so that 전체 문의 현황을 파악할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 회원이 `/inquiries`에 접근하면, THE System SHALL 전체 문의 목록을 작성일시 내림차순, 동일 시각은 ID 내림차순으로 표시한다. ✅
2. THE System SHALL 페이지당 10건으로 서버 사이드 페이지네이션한다. ✅
3. THE System SHALL 요청 페이지 번호를 1 이상 전체 페이지 수 이하로 보정한다. ✅
4. THE System SHALL 문의가 없으면 전체 페이지 수를 1로 처리한다. ✅
5. THE System SHALL 문의 ID, 제목, 조회수, 작성일시, 전체 건수와 현재/전체 페이지를 제공한다. ✅
6. THE System SHALL 이전/다음 페이지 이동 가능 여부를 제공한다. ✅

### Requirement 2: 문의 작성

**User Story:** As an 인증된 회원, I want 제목과 내용을 입력해 문의를 등록할 수 있기를, so that 담당자에게 요청을 전달할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 회원이 `/inquiries/new`에 접근하면, THE System SHALL 문의 작성 폼을 표시한다. ✅
2. THE System SHALL 제목을 필수 입력으로 하고 20자 이하로 제한한다. ✅
3. THE System SHALL 내용을 필수 입력으로 하고 500자 이하로 제한한다. ✅
4. IF 입력값 검증에 실패하면, THEN THE System SHALL 작성 폼을 유지하고 오류 메시지를 표시한다. ✅
5. WHEN 작성이 성공하면, THE System SHALL 현재 로그인 회원을 작성자로 저장하고 조회수를 0으로 초기화한다. ✅
6. THE System SHALL 작성일시를 Asia/Seoul 기준으로 저장한다. ✅
7. WHEN 작성이 성공하면, THE System SHALL 생성된 문의 상세 `/inquiries/{id}`로 이동한다. ✅
8. THE System SHALL 문의 등록 POST에 CSRF 토큰을 적용한다. ✅

### Requirement 3: 문의 상세 조회 및 조회수 증가

**User Story:** As an 인증된 회원, I want 개별 문의의 상세 내용을 볼 수 있기를, so that 문의 내용과 작성자를 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 회원이 `/inquiries/{id}`에 접근하면, THE System SHALL 제목, 내용, 작성자 이름, 조회수, 작성일시 및 연결된 첨부 메타데이터를 표시한다. ✅
2. WHEN 문의 상세를 조회하면, THE System SHALL 조회수를 정확히 1 증가시킨다. ✅
3. THE System SHALL 증가 반영된 조회수를 상세 화면에 표시한다. ✅
4. THE System SHALL 작성자 이름을 회원 테이블과 조인하여 조회한다. ✅
5. IF 존재하지 않는 문의 ID로 접근하면, THEN THE System SHALL HTTP 404를 반환한다. ✅

### Requirement 4: 접근 제어 및 메뉴

**User Story:** As a 시스템, I want 로그인한 회원만 문의 기능을 사용하도록 제한할 수 있기를, so that 미인증 접근을 차단한다.

#### Acceptance Criteria

1. WHEN 미인증 사용자가 `/inquiries/**`에 접근하면, THE System SHALL 로그인 화면으로 이동시킨다. ✅
2. THE System SHALL 로그인한 모든 회원에게 사이드바 `문의 요청` 메뉴를 노출한다. ✅
3. THE System SHALL 문의 화면에서 해당 메뉴를 활성 상태로 표시한다. ✅
4. THE System SHALL 공통 헤더/사이드바 fragment(`fragments/layout`)를 재사용한다. ✅

### Requirement 5: 첨부파일 업로드, 안전한 다운로드 및 승인된 편집 확장

**User Story:** As an 인증된 회원, I want 문의에 검증된 파일을 첨부하고 작성자인 경우 기존 첨부를 안전하게 관리할 수 있기를, so that 근거 자료를 최신 상태로 유지할 수 있다.

#### Acceptance Criteria

1. THE System SHALL `POST /inquiries` multipart 요청에서 제목·내용·선택 첨부파일을 하나의 문의 생성 작업으로 처리하며, 파일이 없더라도 기존 문의 작성은 성공해야 한다. ✅
2. THE System SHALL 각 업로드 파일을 최대 10 MiB로 제한하고, 생성 요청의 첨부 합계 및 저장 후 문의의 전체 첨부 합계를 각각 최대 20 MiB로 제한한다. 기존 구현은 생성 요청 합계를 검증한다; 편집 시 저장 후 합계 검증은 ✅ (자동 검증 미실시)이다.
3. THE System SHALL 문의당 저장 첨부파일 수를 최대 5개로 제한한다. 이 값은 이전의 3개 제한을 대체한다. 생성 및 편집 모두 서버에서 최종 저장 개수를 검증해야 한다. ✅ (자동 검증 미실시)
4. THE System SHALL PDF(`application/pdf`), PNG(`image/png`), JPEG(`image/jpeg`)만 허용하며, 클라이언트 MIME을 신뢰하지 않고 확장자·선언 MIME·magic bytes가 모두 일치할 때만 허용한다. ✅
5. THE System SHALL 빈 파일, 허용하지 않은 확장자·콘텐츠, 손상된 서명, 제어문자 또는 경로 구분자(`/`, `\\`)가 있는 표시 파일명을 거부한다. 표시 파일명은 NFC 정규화 후 255자 이하로 검증한다. ✅
6. THE System SHALL 사용자 파일명을 실제 저장 경로나 저장 키로 사용하지 않고 UUID 저장 키와 웹 정적 리소스 밖의 비공개 저장소를 사용한다. ✅
7. WHEN 첨부가 포함된 문의가 생성되면, THE System SHALL 상세에 표시 파일명·크기·인증된 회원용 다운로드 링크를 제공한다. ✅
8. WHEN 인증된 회원이 `GET /inquiries/{inquiryId}/attachments/{attachmentId}/download`에 접근하면, THE System SHALL inquiry-attachment 소속을 검증한 뒤 전체 파일을 메모리에 적재하지 않는 스트리밍 다운로드를 제공한다. ✅
9. THE System SHALL 다운로드 응답에 검증된 저장 MIME, UTF-8 안전 `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-store`를 설정한다. ✅
10. IF attachment가 없거나 요청 inquiry에 속하지 않거나 저장 파일이 없으면, THEN THE System SHALL 저장 키·경로·내부 오류를 노출하지 않는 동일한 HTTP 404를 반환한다. ✅
11. THE System SHALL 등록·메타데이터 저장 실패 시 생성된 staging/final 파일을 보상 삭제하고, DB 비참조 UUID 파일의 안전한 운영 정리 경로를 제공한다. ✅
12. WHEN 인증된 작성자가 `GET /inquiries/{id}/edit`에 접근하면, THE System SHALL 제목·내용과 기존 첨부 목록을 포함한 편집 폼을 표시한다. 기존 첨부는 명시적 삭제가 없으면 보존되어야 한다. ✅ (자동 검증 미실시)
13. WHEN 인증된 작성자가 문의 편집을 제출하면, THE System SHALL 제목·내용 수정과 신규 첨부 추가를 하나의 편집 작업(`POST /inquiries/{id}/edit`)으로 처리한다. 신규 첨부가 없어도 본문 편집은 가능해야 한다. 기존 첨부의 개별 삭제는 이 편집 요청이 아니라 별도의 즉시 삭제 엔드포인트(요구사항 21)로 처리한다. ✅ (자동 검증 미실시)
14. THE System SHALL 편집 결과의 첨부 수가 5개 이하이고, 현재 저장된 기존 첨부와 새 업로드 파일을 합친 총 크기가 20 MiB 이하인지 서버에서 검증한다. 편집 POST는 삭제를 수행하지 않으므로, 개수·크기 계산의 기준은 요청 시점에 실제로 저장되어 있는 기존 첨부다. 🔶
15. THE System SHALL 편집 요청에서 신규 파일마다 10 MiB, 파일 수, 총 요청 크기, 파일명, 확장자·선언 MIME·magic bytes를 서버 측에서 검증한다. HTML `accept`, CSS, 클라이언트 파일 목록은 보조 UX이며 권한 또는 제한 검증을 대체할 수 없다. ✅ (자동 검증 미실시)
16. THE System SHALL 작성자 본인만 `/inquiries/{id}/edit` 및 개별 첨부 삭제(`/inquiries/{id}/attachments/{attachmentId}/delete`)를 수행하도록 서버에서 검증한다. 미인증 사용자는 로그인 흐름으로, 인증됐지만 작성자가 아닌 사용자는 권한 거부(403) 응답으로 처리하며, 어느 경우에도 파일을 변경·삭제하지 않는다. ✅ (자동 검증 미실시)
17. THE System SHALL 편집·개별 첨부 삭제를 모두 상태 변경 POST 요청으로 취급하고 CSRF 보호를 적용한다. ✅ (자동 검증 미실시)
18. IF 편집 중 검증, 파일 I/O 또는 DB 저장에 실패하면, THEN THE System SHALL 문의·기존 첨부 메타데이터·기존 파일을 변경하지 않고 신규 staging/final 파일을 보상 삭제한다. 개별 삭제 요청에서 삭제 대상 파일은 DB 커밋 후에만 제거하며 제거 실패는 보안 로그와 orphan reconciliation 대상으로 남긴다. ✅ (자동 검증 미실시)
19. THE System SHALL 첨부 안내 문구와 첨부파일 목록 UI에만 `12px` 이상의 접근 가능한 글자 크기를 적용하고, 그 외 문의 화면의 글자 크기는 변경하지 않는다. ✅ (자동 검증 미실시)
20. THE System SHALL 문의 상세 및 편집 화면의 기존 첨부 목록에 표시하는 첨부 크기를 사람이 읽기 쉬운 이진(1024 기반) 단위(B/KB/MB/…)로 크기 규모에 따라 선택해 표시하되, KB 이상은 소수점 1자리, 바이트 단위는 정수로 나타낸다. 이 변환은 표시 전용이며 저장된 바이트 값은 변경하지 않고, 저장 키·실제 파일 경로를 노출하지 않는다. 🔶
21. WHEN 인증된 작성자가 편집 화면에서 특정 첨부의 삭제를 요청하면, THE System SHALL `POST /inquiries/{id}/attachments/{attachmentId}/delete`로 해당 첨부 하나만 즉시 삭제한다. THE System SHALL 삭제 전 `(inquiryId, attachmentId)` 소속을 검증하고, 메타데이터 삭제(DB)와 커밋 후 저장 파일 제거를 수행하며, 성공 시 `GET /inquiries/{id}/edit`로 리다이렉트하여 편집 화면을 갱신한다. THE System SHALL 마지막 첨부의 삭제도 허용한다(첨부 0개 상태 허용). 🔶
22. IF 개별 삭제 요청의 첨부가 없거나 요청 문의에 속하지 않으면, THEN THE System SHALL 저장 키·경로·내부 오류를 노출하지 않는 일반 HTTP 404를 반환한다. IF 커밋 후 저장 파일 제거가 실패하면, THEN THE System SHALL 경로를 노출하지 않고 보안 로그·orphan reconciliation 대상으로 남기되 DB 삭제 결과는 유지한다. 🔶

### Requirement 6: 문의 목록의 첨부 표시 (예정)

**User Story:** As an 인증된 회원, I want 문의 목록에서 첨부파일이 있는 문의를 즉시 식별할 수 있기를, so that 상세를 열기 전에 첨부 여부를 알 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 회원이 `/inquiries` 목록을 조회하면, THE System SHALL 하나 이상의 첨부가 연결된 문의 행에 단순한 시각적 첨부 표시를 제공한다. 🔶
2. THE System SHALL 해당 표시의 접근 가능한 이름으로 `첨부파일`을 제공하여, 아이콘만으로 정보를 전달하지 않는다. 🔶
3. WHEN 문의에 첨부가 없으면, THE System SHALL 첨부 표시와 그에 대응하는 스크린 리더 텍스트를 렌더링하지 않는다. 🔶
4. THE System SHALL 목록 페이지 데이터 조회에서 행별 추가 첨부 조회(N+1)를 수행하지 않고, 단일 목록 쿼리의 `EXISTS` 또는 동등한 집계 결과로 첨부 여부를 제공한다. 🔶
5. THE System SHALL 목록 응답·템플릿·접근성 표시 어디에도 attachment ID, storage key, 실제 파일 경로 또는 다운로드 경로를 노출하지 않는다. 🔶
6. WHEN 문의 목록이 비어 있으면, THE System SHALL 기존의 빈 목록·페이지네이션 동작을 유지하고 첨부 표시를 렌더링하지 않는다. 🔶

## 업무 규칙 요약

| 번호 | 규칙 | 상태 |
|---|---|---|
| IR-01 | 문의는 로그인 회원만 작성·조회한다 | ✅ |
| IR-02 | 제목은 필수, 최대 20자이며 내용은 필수, 최대 500자다 | ✅ |
| IR-03 | 신규 문의의 작성자는 세션 회원이고 조회수는 0이다 | ✅ |
| IR-04 | 상세 조회마다 조회수가 1 증가한다 | ✅ |
| IA-01 | 허용 타입은 PDF, PNG, JPEG이며 확장자·선언 MIME·magic bytes가 일치해야 한다 | ✅ |
| IA-02 | 각 업로드 파일은 최대 10 MiB다 | ✅ |
| IA-03 | 문의의 저장 첨부 수는 최대 5개, 저장 후 전체 첨부 크기는 최대 20 MiB다 | ✅ (자동 검증 미실시) |
| IA-04 | 편집은 기존 첨부를 보존하고, 작성자만 새 파일을 추가할 수 있다. 개별 삭제는 편집 화면 내 즉시 삭제 엔드포인트로 처리한다 | ✅ (자동 검증 미실시) |
| IA-05 | 편집(본문·추가)과 개별 즉시 삭제는 모두 인증·CSRF·서버 권한 검증과 실패 보상/커밋 후 파일 정리를 적용한다 | ✅ (자동 검증 미실시) |
| IA-08 | 개별 첨부 삭제는 `POST /inquiries/{id}/attachments/{attachmentId}/delete`로 소속 검증 후 즉시 수행하고 편집 화면으로 리다이렉트하며, 마지막 첨부 삭제도 허용한다 | 🔶 |
| IA-06 | 첨부 안내와 파일 목록만 12px로 축소한다 | ✅ (자동 검증 미실시) |
| IA-07 | 상세·편집 화면의 첨부 크기는 이진(1024 기반) 단위로 규모에 맞춰 표시하고(KB 이상 소수점 1자리, 바이트는 정수) 표시 전용이며 저장 바이트를 바꾸지 않고 저장 키·경로를 노출하지 않는다 | 🔶 |
| IL-01 | 문의 목록은 첨부 존재 여부만 접근 가능하게 표시하고, 단일 목록 쿼리로 조회하며 파일 식별자·경로를 노출하지 않는다 | 🔶 |

## 확인이 필요한 사항

| 번호 | 확인 항목 | 현재 기준 |
|---|---|---|
| Q1 | 문의 공개 범위 | 로그인한 모든 회원이 목록·상세·다운로드 가능 |
| Q2 | 조회수 중복 증가 방지 | 현재 접근마다 증가 |
| Q3 | 관리자 답변·처리 상태 | 미구현 |
| Q4 | 작성자 외 ADMIN의 편집 권한 | 승인 범위는 작성자 본인만이며 ADMIN 예외는 없음 |
| Q5 | 첨부 저장소 백업·보존·정리 주기 | 운영 환경에서 결정 필요 |
| Q6 | 악성코드 검사·객체 스토리지 전환 | 현재 범위 밖 |

## 현재 구현 및 검증 범위

문의 작성·목록·상세, 안전한 첨부 업로드/다운로드와 함께 작성자 본인의 편집이 구현되어 있다. 편집은 `GET /inquiries/{id}/edit` 및 CSRF 보호 multipart `POST /inquiries/{id}/edit`를 사용하며, 제목·내용 변경, retain-by-default 기존 첨부, 신규 파일 추가를 한 작업으로 처리한다. 기존 첨부의 개별 삭제는 이 편집 요청이 아니라 별도의 CSRF 보호 즉시 삭제 엔드포인트 `POST /inquiries/{id}/attachments/{attachmentId}/delete`로 처리한다(이전 `deleteAttachmentIds` 지연 삭제 방식을 대체함). 서버는 PDF/PNG/JPEG의 확장자·선언 MIME·magic bytes를 확인하고, 파일당 10 MiB, 최종 5개, 현재 저장분을 포함한 최종 20 MiB를 적용한다. 첨부 안내와 목록에만 12px 스타일을 적용한다.

새 UUID 비공개 파일은 저장·DB 작업 실패 시 보상 삭제한다. 개별 삭제 엔드포인트는 `(inquiryId, attachmentId)` 소속을 검증한 뒤 메타데이터를 삭제하고, 저장 파일은 DB 커밋 후 제거하며, 제거 실패는 경로를 드러내지 않고 orphan reconciliation 로그 대상으로 남긴다. 마지막 첨부의 삭제(첨부 0개 상태)도 허용한다. 현 `inquiry_attachments`의 행별 metadata schema는 이 aggregate invariant와 호환되므로 edit 전용 Liquibase changeSet은 추가되지 않았고, 기존 metadata·storage key·다운로드는 보존된다.

**예정된 목록 표시 확장:** 문의 목록에는 첨부가 하나 이상인 행만 단순 시각 표시와 스크린 리더용 `첨부파일` 이름을 추가한다. 첨부가 없는 행과 빈 목록에는 표시를 만들지 않는다. 목록 read model은 attachment metadata나 파일 위치를 싣지 않고, 목록 SQL의 `EXISTS` 또는 동등한 집계로 `hasAttachments` boolean만 산출해 N+1 조회를 방지한다.

**검증 상태:** 9.7.8 및 9.7.9는 사용자 요청으로 건너뛰었다. 따라서 편집 확장의 단위/속성, controller/integration/migration 회귀 검증은 수행되지 않았으며, 이 문서화 작업은 테스트·빌드·마이그레이션 통과를 의미하지 않는다.

## 문서 관계

| 문서 | 범위 |
|---|---|
| [`member-auth-baseline`](../member-auth-baseline/) | 권한 구분 이전 초기 시스템 스냅샷 |
| [`member-access-management`](../member-access-management/) | 역할·상태·권한 관리·감사 로그 |
| **본 스펙(`inquiry-management`)** | 문의 작성·목록·상세·첨부 및 승인된 편집 확장 |
