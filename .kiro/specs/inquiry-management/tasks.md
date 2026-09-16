# Implementation Plan: inquiry-management

> **Spec:** inquiry-management · **종류:** living spec

## Overview

문의의 기본 작성·목록·상세·접근 제어와 안전한 첨부 업로드/다운로드를 보존하면서, 9.7은 작성자 본인의 문의 편집으로 확장했다. 구현된 범위는 `GET`/multipart `POST /inquiries/{id}/edit`, retain-by-default 기존 첨부, `deleteAttachmentIds` 개별 삭제, 최종 5개·20 MiB 및 신규 파일당 10 MiB 제한, PDF/PNG/JPEG 검증, 작성자/CSRF 보호, 파일 보상 정리, 첨부 영역에 한정된 12px UI다. 이후 9.10에서 개별 삭제 방식을 편집 화면 내 **즉시 삭제 엔드포인트**(`POST /inquiries/{id}/attachments/{attachmentId}/delete`)로 대체하며, 이전 `deleteAttachmentIds` 지연 삭제 서술은 폐기한다(9.7의 완료 이력 자체는 보존한다).

- `[x]` — 구현 또는 문서화 완료
- `[-]` — 사용자 요청으로 건너뜀(검증되지 않음)
- `[ ]` — 향후 작업

> **검증 상태:** 사용자의 명시적 요청으로 9.7.8과 9.7.9는 건너뛰었다. 이 작업(9.7.10)은 문서만 갱신하며 테스트·빌드·마이그레이션 실행을 수행하지 않았으므로, 편집 확장에 대한 자동 검증 또는 빌드 통과를 주장하지 않는다.

## Tasks

- [x] 1. 도메인·스키마
  - [x] 1.1 `Inquiry` 도메인 record(id/memberId/title/content/viewCount/createdAt)
  - [x] 1.2 Liquibase changeSet `8-create-inquiries`로 `inquiries` 테이블 생성(members FK)
  - [x] 1.3 기존 테이블·데이터 보존(precondition 감지, 재실행 안전)

- [x] 2. 문의 작성
  - [x] 2.1 `/inquiries/new` 작성 폼, `POST /inquiries` 등록
  - [x] 2.2 제목 필수·20자 이하, 내용 필수·500자 이하 검증
  - [x] 2.3 작성자=세션 회원, 조회수 0, 작성일시 Asia/Seoul
  - [x] 2.4 성공 시 상세로 리다이렉트, POST에 CSRF 적용

- [x] 3. 문의 목록
  - [x] 3.1 `GET /inquiries` 최신순, 페이지당 10건, 서버 페이지네이션
  - [x] 3.2 페이지 번호 [1, totalPages] 보정, 빈 목록 시 totalPages=1
  - [x] 3.3 제목/조회수/작성일시 표시, 이전·다음 이동 제공

- [x] 4. 문의 상세
  - [x] 4.1 `GET /inquiries/{id}` 상세 표시(작성자 이름 조인)
  - [x] 4.2 조회 시 조회수 증가 및 반영된 값 표시 — 작성자 본인 조회는 증가하지 않음(셀프 카운트 방지), 그 외 회원 조회는 +1. `getDetail(id, viewerMemberId)`로 조회자를 전달하고 작성자 ID와 비교
  - [x] 4.3 존재하지 않는 문의 → HTTP 404

- [x] 5. 접근 제어·메뉴
  - [x] 5.1 `/inquiries/**` 로그인 필수(미인증 로그인 이동)
  - [x] 5.2 공통 사이드바에 `문의 요청` 메뉴 추가, 활성 표시
  - [x] 5.3 공통 헤더/사이드바 fragment 재사용

- [x] 6. 기존 테스트
  - [x] 6.1 작성 서비스 단위 테스트
  - [x] 6.2 작성 폼 검증 테스트
  - [x] 6.3 컨트롤러 테스트(목록/폼/상세/404)
  - [x] 6.4 Liquibase 마이그레이션 테스트(멱등·기존 데이터 보존·FK)

- [x] 7. 기존 문서
  - [x] 7.1 README에 문의 기능·경로·스키마·테스트 반영
  - [x] 7.2 steering(structure/tech)에 문의 구성 반영
  - [x] 7.3 본 문의 스펙(requirements/design/tasks) 신설

- [x] 9.5 첨부파일 업로드·안전한 다운로드 (기존 완료 범위)
  - [x] 9.5.1 첨부 도메인·포트 정의
  - [x] 9.5.2 Liquibase `inquiry_attachments` 스키마 changeSet 추가
  - [x] 9.5.3 MyBatis attachment persistence adapter 구현
  - [x] 9.5.4 private local storage adapter와 환경 설정 구현
  - [x] 9.5.5 서버 측 attachment validator 구현(기존 최대 3개, 파일당 10 MiB, 합계 20 MiB)
  - [x] 9.5.6 multipart 생성 use case와 web adapter 확장
  - [x] 9.5.7 상세 조회·템플릿에 attachment metadata 표시
  - [x] 9.5.8 다운로드 use case·controller와 안전 헤더 구현
  - [x] 9.5.9 보상 정리·운영 재조정 작업 구현
  - [x] 9.5.10 첨부 단위·경계 테스트 작성
  - [x] 9.5.11 controller·integration·migration 테스트 작성
  - [x] 9.5.12 구현 문서 최신화

- [x] 9.7 승인된 문의 편집·첨부 확장
  - [x] 9.7.1 편집 도메인·인바운드/아웃바운드 포트를 추가한다: `UpdateInquiryUseCase`, command/result, 작성자 소유권 조회, inquiry 본문 update, attachment aggregate(list/count/size) 및 verified metadata delete를 정의한다. Spring·`MultipartFile`·`Path`는 application/domain 경계를 넘기지 않는다.
  - [x] 9.7.2 MyBatis persistence adapter와 XML을 확장한다: 작성자 확인을 포함한 inquiry 조회, 제목·내용 update, inquiry별 attachment aggregate/list, `(inquiryId, attachmentId)` 소속 검증 delete를 추가한다. 동시 편집으로 5개/20 MiB를 초과하지 않도록 대상 inquiry aggregate의 write-time locking 또는 동등한 일관성 전략을 구현한다.
  - [x] 9.7.3 기존 attachment validator와 multipart configuration을 개정한다: 생성·편집 모두 최종 최대 5개를 서버에서 적용하고, 파일당 10 MiB·신규 요청 합계 제한·편집 후 보존분 포함 20 MiB를 검증한다. 기존 3개 상수·메시지·테스트를 5개 기준으로 교체한다.
  - [x] 9.7.4 `InquiryService` 또는 분리된 편집 application service를 구현한다: actor=author 확인, 기존 첨부 retain-by-default, 삭제 ID의 inquiry 소속 검증, 신규 upload 검증, 최종 count/size 계산, 본문/metadata 변경의 DB 원자성, 신규 file failure 보상 및 커밋 후 삭제 file failure logging/reconciliation을 구현한다.
  - [x] 9.7.5 웹 adapter와 폼을 구현한다: `GET /inquiries/{id}/edit`, CSRF 적용 `POST /inquiries/{id}/edit` multipart, `deleteAttachmentIds`, safe error redisplay 및 성공 detail redirect를 추가한다. 미인증은 기존 로그인 흐름, 비작성자는 403으로 처리하고 어떤 변경도 수행하지 않는다.
  - [x] 9.7.6 inquiry detail/edit templates와 scoped CSS를 구현한다: 작성자에게만 edit 진입을 표시하고 기존 첨부의 개별 삭제 선택·새 파일 추가·제한 안내를 제공한다. 첨부 안내와 첨부파일 목록에만 `font-size: 12px`를 적용하며 다른 문의 UI 글자 크기는 변경하지 않는다.
  - [x] 9.7.7 Liquibase 및 배포 호환성을 확인·구현한다: 현재 `inquiry_attachments` 스키마가 변경 없이 5개/20 MiB service invariant를 지원함을 검증한다. 기존 DB에 3개 제한 제약/트리거가 있는 배포 경로를 탐지하고 필요한 제거·완화 changeSet과 precondition을 추가하며, 기존 metadata/storage key/download는 보존한다.
  - [x] 9.7.8 단위 및 속성 기반 테스트를 작성한다: 5/6개 경계, 파일당 10 MiB, 보존+신규 합계 20 MiB 경계, retain-by-default, 개별 삭제, 추가와 삭제 동시 요청, 비작성자/잘못된 삭제 ID, 실패 보상을 검증한다. **Validates: Requirements 5.3, 5.12–5.18** — 사용자 요청으로 건너뜀; 작성·실행·검증하지 않음.
  - [x] 9.7.9 controller·integration·migration 회귀 테스트를 작성한다: author edit GET/POST, multipart·CSRF·미인증·403, 서버 제한 우회 방지, 12px 스타일 범위, DB/파일 보상, 동시 편집 일관성, Liquibase 멱등성, 기존 첨부 다운로드 보존을 실제 private temp storage와 H2로 검증한다. **Validates: Requirements 5.3, 5.12–5.19** — 사용자 요청으로 건너뜀; 작성·실행·검증하지 않음.
  - [x] 9.7.10 living spec과 운영 문서를 실제 코드·API·제약·마이그레이션 결과에 맞춰 갱신했다. 9.7.8·9.7.9가 사용자 요청으로 건너뛰어 검증되지 않았고, 이 문서 작업에서는 테스트·빌드·마이그레이션을 실행하지 않았음을 명시한다.

- [x] 9.8 문의 목록 첨부 존재 표시
  - [x] 9.8.1 목록 read model과 query port에 `hasAttachments` boolean을 추가한다. attachment ID, storage key, 파일명·경로, 다운로드 URL 또는 attachment metadata는 목록 projection에 포함하지 않는다.
  - [x] 9.8.2 MyBatis 목록 SQL/XML을 확장해 기존 페이지네이션·정렬을 보존하면서 `EXISTS` 또는 동등한 단일 집계로 `hasAttachments`를 계산한다. 목록 행별 attachment 조회를 추가하지 않는다.
  - [x] 9.8.3 문의 목록 템플릿/UI에 `hasAttachments=true` 행만 단순 첨부 표시와 접근 가능한 `첨부파일` 이름을 렌더링한다. 첨부 없는 행과 빈 목록에는 아이콘·숨김 텍스트·placeholder를 렌더링하지 않는다.
  - [-] 9.8.4 목록 query/mapper, controller/template 접근성 및 Property 8 회귀 테스트를 작성한다: 첨부 있음·없음, 빈 목록, 단일 목록 쿼리, `첨부파일` 접근 가능한 이름, attachment ID/storage key/path 비노출을 검증한다. **Validates: Requirements 6.1–6.6**

- [x] 9.9 첨부 크기 사람이 읽기 쉬운 표시 (코드 구현 전용)
  - [x] 9.9.1 저장된 첨부 바이트 값을 이진(1024 기반) 단위(B/KB/MB/…)로 규모에 맞춰 변환하는 표시 전용 포매팅을 추가한다. KB 이상은 소수점 1자리, 바이트는 정수로 나타내며 저장 바이트 값·검증 로직은 변경하지 않는다. **Implements: Requirements 5.20 (IA-07), Design 속성 9**
  - [x] 9.9.2 `inquiry/detail.html`과 `inquiry/form.html`의 기존 첨부 목록에 포맷된 크기를 표시한다. 저장 키·실제 파일 경로·다운로드 주소를 노출하지 않고 크기 이외 첨부 정보 노출 방침은 기존 설계를 유지한다. **Implements: Requirements 5.20 (IA-07), Design 속성 9**

- [x] 9.10 편집 화면 내 개별 첨부 즉시 삭제 (코드 구현 전용)
  - [x] 9.10.1 개별 삭제 인바운드 포트를 추가한다: `DeleteInquiryAttachmentUseCase`와 `DeleteInquiryAttachmentCommand`(inquiryId, attachmentId, 요청 회원 ID)를 정의한다. Spring·`MultipartFile`·`Path`는 application/domain 경계를 넘기지 않는다. 편집 POST의 `deleteAttachmentIds` 기반 지연 삭제를 대체한다. **Implements: Requirements 5.21, 5.22, Design 속성 6**
  - [x] 9.10.2 개별 삭제 애플리케이션 서비스를 구현한다: actor=author 확인, `(inquiryId, attachmentId)` 소속 검증, 소속 불일치 시 일반 404, DB 메타데이터 삭제와 커밋 후 저장 파일 제거, 커밋 후 제거 실패 시 경로 미노출 보안 로그·orphan reconciliation, 마지막 첨부 삭제(첨부 0개 상태) 허용을 구현한다. 편집 POST(`UpdateInquiryUseCase`)에서 삭제 처리·`deleteAttachmentIds` 계산을 제거하고 최종 개수·크기 기준을 현재 저장분 + 새 파일로 변경한다. **Implements: Requirements 5.13, 5.14, 5.21, 5.22, Design 속성 5, 6**
  - [x] 9.10.3 퍼시스턴스 어댑터·XML을 확인·정비한다: 기존 `(inquiryId, attachmentId)` 소속 검증 delete 매퍼를 즉시 삭제 흐름에서 재사용하고, 편집 aggregate 계산에서 삭제 반영 로직 의존을 제거한다. `inquiry_attachments` 스키마·저장 키·다운로드 호환성은 변경하지 않는다. **Implements: Requirements 5.21, 5.22**
  - [x] 9.10.4 웹 어댑터에 CSRF 보호 `POST /inquiries/{id}/attachments/{attachmentId}/delete` 엔드포인트를 추가한다: 작성자 검증, 미인증 로그인 리다이렉트, 비작성자 403, 소속 불일치 404, 성공 시 `GET /inquiries/{id}/edit` 리다이렉트를 구현한다. 편집 POST(`InquiryController`, `InquiryForm`)에서 `deleteAttachmentIds` 바인딩·처리를 제거한다. **Implements: Requirements 5.16, 5.17, 5.21, 5.22**
  - [x] 9.10.5 편집 템플릿을 수정한다: 기존 첨부 목록의 각 항목에 CSRF 토큰을 포함한 개별 삭제 POST 폼(삭제 버튼)을 두고, 편집 multipart 폼에서 `deleteAttachmentIds` 입력을 제거한다. 첨부 안내·목록 12px 범위와 다른 UI 글자 크기는 변경하지 않는다. **Implements: Requirements 5.19, 5.21**
  - [x] 9.10.6 단위·컨트롤러·통합 테스트를 작성한다: 작성자 즉시 삭제 성공(편집 리다이렉트), 소속 불일치 404, 비작성자 403, 미인증 리다이렉트, CSRF 실패 403, 마지막 첨부 삭제 허용, 커밋 후 파일 제거 실패 로깅, 편집 POST가 더 이상 삭제를 수행하지 않음(현재 저장분 + 새 파일 기준 5개/20 MiB 경계)을 실제 private temp storage와 H2로 검증한다. 9.10 리팩터로 stale 해진 컨트롤러/서비스 테스트(`InquiryControllerTest`, `InquiryServiceTest`)를 작성자 전용 편집 규칙과 현재 컨트롤러 동작에 맞춰 정정했다. `./mvnw -o test` 전체 139개 통과(1개 skip: Windows symlink 미지원 `LocalInquiryAttachmentStorageTest`, 본 작업과 무관). **Validates: Requirements 5.13, 5.14, 5.16, 5.17, 5.21, 5.22**

## Deferred product decisions (not implementation tasks)

| Item | Current policy |
|---|---|
| 문의 공개 범위 | 로그인 회원 전체 조회·다운로드 |
| 조회수 중복 증가 방지 | 현재 접근마다 증가 |
| 관리자 답변/처리 상태 | 미구현 |
| ADMIN의 타인 문의 편집 | 승인 범위 밖; 작성자 본인만 편집 |
| 첨부 저장소 백업/보존/정리 주기 | 운영 환경에서 결정 |

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4", "3.1", "3.2", "3.3", "4.1", "4.2", "4.3", "5.1", "5.2", "5.3"] },
    { "id": 2, "tasks": ["6.1", "6.2", "6.3", "6.4"] },
    { "id": 3, "tasks": ["7.1", "7.2", "7.3"] },
    { "id": 4, "tasks": ["9.5.1", "9.5.2", "9.5.4", "9.5.5"] },
    { "id": 5, "tasks": ["9.5.3"] },
    { "id": 6, "tasks": ["9.5.6", "9.5.9"] },
    { "id": 7, "tasks": ["9.5.7", "9.5.8"] },
    { "id": 8, "tasks": ["9.5.10", "9.5.11"] },
    { "id": 9, "tasks": ["9.5.12"] },
    { "id": 10, "tasks": ["9.7.1", "9.7.2", "9.7.3", "9.7.7"] },
    { "id": 11, "tasks": ["9.7.4"] },
    { "id": 12, "tasks": ["9.7.5", "9.7.6"] },
    { "id": 13, "tasks": ["9.7.8", "9.7.9"] },
    { "id": 14, "tasks": ["9.7.10"] },
    { "id": 15, "tasks": ["9.8.1"] },
    { "id": 16, "tasks": ["9.8.2"] },
    { "id": 17, "tasks": ["9.8.3"] },
    { "id": 18, "tasks": ["9.8.4"] },
    { "id": 19, "tasks": ["9.9.1"] },
    { "id": 20, "tasks": ["9.9.2"] },
    { "id": 21, "tasks": ["9.10.1"] },
    { "id": 22, "tasks": ["9.10.2", "9.10.3"] },
    { "id": 23, "tasks": ["9.10.4", "9.10.5"] },
    { "id": 24, "tasks": ["9.10.6"] }
  ]
}
```

The completed waves preserve the baseline and the implemented edit-extension history. Waves 15–18 add the list projection, single-query persistence mapping, accessible UI, and regression/property coverage for the planned attachment indicator. Waves 19–20 add the display-only human-readable attachment size formatting and its use in the detail/edit existing-attachment lists. Waves 21–24 replace the prior `deleteAttachmentIds` deferred deletion with the immediate per-attachment delete endpoint (use case/port, service·persistence, controller endpoint·edit template change, and tests), while the 9.7 completion history is preserved.

## Document Relations

| Document | Scope |
|---|---|
| [`member-auth-baseline`](../member-auth-baseline/) | Initial authentication snapshot |
| [`member-access-management`](../member-access-management/) | Role, status, and access management |
| **This spec** | Inquiry lifecycle, existing safe attachments, and pending approved author edit extension |
