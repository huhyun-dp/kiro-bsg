# 문의 요청(Inquiry) 설계 문서

> **Spec:** inquiry-management · **종류:** living spec

## Overview

문의 요청 기능은 로그인한 회원이 문의를 작성하고, 최신순 목록·상세를 조회하며, 검증된 PDF/PNG/JPEG 파일을 비공개 저장소에서 첨부·다운로드하는 기능이다. 기존 첨부 생성/다운로드 구현은 유지한다.

승인된 확장은 작성자 본인에게 문의 편집을 제공한다. 편집은 기존 첨부를 기본적으로 유지하고, 새 파일을 추가하거나 연결된 파일을 개별 삭제할 수 있다. 최종 첨부는 최대 5개, 총 저장 크기는 최대 20 MiB이며, 새 업로드 파일 하나는 최대 10 MiB다.

예정된 목록 확장은 첨부가 하나 이상인 문의 행만 식별 가능한 표시를 추가한다. 이 표시는 파일의 존재 여부만 전달하며 attachment ID, storage key, 파일명·경로, 다운로드 URL 또는 기타 attachment metadata를 목록 read model에 추가하지 않는다.

| 사용자 | 목록·상세·다운로드 | 편집·첨부 삭제 |
|---|---|---|
| 미로그인 | 불가 → 로그인 | 불가 → 로그인 |
| 로그인 회원 | 가능 | 불가(작성자 아님) |
| 문의 작성자 | 가능 | 가능 |

## Architecture

기존 헥사고날 구조와 CQRS 분리를 유지한다. domain은 순수 Java, application service는 Spring 어노테이션 없이 port만 의존한다.

```text
Browser
  └─ InquiryController (web adapter; multipart/CSRF/model/response)
       ├─ CreateInquiryUseCase / UpdateInquiryUseCase
       ├─ InquiryQueryUseCase / DownloadInquiryAttachmentUseCase
       └─ InquiryAttachmentUploadValidator
             ↓
       InquiryService / InquiryAttachmentDownloadService
             ↓
       InquiryRepository + InquiryAttachmentRepository
       InquiryQueryRepository + InquiryAttachmentQueryRepository
       InquiryAttachmentStorage
             ↓
       MyBatis metadata adapters + LocalInquiryAttachmentStorage
             ↓
       MySQL (inquiries, inquiry_attachments) + private storage root
```

편집은 기존 첨부의 데이터와 새 파일의 staging/final 파일을 조정해야 하므로, DB 트랜잭션만으로 파일시스템 원자성을 가정하지 않는다. 신규 파일은 먼저 staging/final 저장 후 DB 작업 실패 시 보상 삭제한다. 기존 첨부 삭제의 실제 파일 제거는 DB 커밋 후에 수행하며, 제거 실패는 보안 로그 및 재조정 대상으로 남긴다. 따라서 검증·DB 실패는 기존 문의/첨부를 보존한다.

## HTTP Components and Flows

| 경로 | 메서드 | 인증 | CSRF | 책임 |
|---|---|---:|---:|---|
| `/inquiries` | GET | 필요 | - | 목록 |
| `/inquiries/new` | GET | 필요 | - | 생성 폼 |
| `/inquiries` | POST multipart | 필요 | 필요 | 문의 및 초기 첨부 생성 |
| `/inquiries/{id}` | GET | 필요 | - | 상세/조회수 증가 |
| `/inquiries/{id}/edit` | GET | 필요 + 작성자 | - | 편집 폼 |
| `/inquiries/{id}/edit` | POST multipart | 필요 + 작성자 | 필요 | 제목·내용 수정, 신규 첨부 추가, 선택된 기존 첨부 삭제 |
| `/inquiries/{inquiryId}/attachments/{attachmentId}/download` | GET | 필요 | - | 소속 확인 후 스트리밍 다운로드 |

편집 POST는 `deleteAttachmentIds`(0개 이상)를 함께 받아 개별 삭제를 표현한다. 별도 삭제 API는 만들지 않는다. 이 방식은 제목/내용 수정, 새 업로드, 삭제가 하나의 검증·저장 workflow로 처리되도록 하며, 각 삭제 ID는 서버에서 대상 문의 소속을 확인한다.

### Creation and edit attachment validation

1. Web adapter는 `attachments`와 `deleteAttachmentIds`를 request DTO로 바인딩하되, `MultipartFile`, `Path`, 원시 파일 스트림을 application/domain 경계 너머로 전달하지 않는다.
2. Validator는 모든 신규 파일에 대해 비어 있지 않음, NFC 표시 파일명 255자 이하, 제어문자·`/`·`\\` 없음, 허용 확장자, 선언 MIME 및 magic bytes 일치를 검증한다.
3. Validator는 신규 파일 각각 10 MiB 이하와 요청 전체 신규 업로드 합계 20 MiB 이하를 검증한다. multipart parser는 파일당 10 MiB, 요청당 21 MiB 제한을 계속 적용한다.
4. Update service는 대상 문의의 기존 첨부를 조회하고, 삭제 요청 ID가 모두 해당 문의에 속하는지 확인한다. 삭제 후 보존되는 첨부 수·크기와 유효 신규 첨부를 합산한다.
5. `retainedCount + newCount <= 5` 및 `retainedSize + newUploadSize <= 20 MiB`를 server-side에서 확인한다. 삭제되지 않은 기존 첨부는 항상 수·크기에 포함한다.
6. 모든 검증 성공 후에만 신규 파일을 UUID storage key로 private staging/final에 저장하고 DB update, metadata insert, metadata delete를 단일 DB transaction에서 실행한다.
7. DB 커밋 후 삭제된 metadata의 final 파일을 storage key로 제거한다. 실패는 사용자에게 파일 경로를 노출하지 않고 보안 로그와 cleanup/reconciliation 대상으로 기록한다.

### Authorization and failure behavior

- `/inquiries/**`의 기존 `AuthenticationInterceptor` 보호를 유지한다.
- edit GET/POST의 controller 또는 dedicated ownership policy는 세션 회원 ID와 inquiry.memberId를 비교한다. 작성자가 아니면 HTTP 403이며, validation/storage/database 작업을 시작하지 않는다.
- edit POST는 기존 `CsrfTokenFilter`로 보호한다. CSRF 실패 시 HTTP 403이며 DB/파일 변경은 없다.
- attachment ID가 대상 inquiry에 없으면 요청을 400(편집 폼 오류)으로 처리한다. 다운로드의 존재·소속·저장 파일 부재는 기존처럼 동일한 404다.
- 검증 실패는 폼을 유지하며, 새 파일/metadata를 남기지 않는다. I/O 또는 DB 실패 시 신규 staging/final을 보상 삭제하고 기존 attachment를 보존한다.

## Data Models and Ports

### Existing models (preserved)

- `Inquiry(id, memberId, title, content, viewCount, createdAt)`
- `InquiryAttachment(id, inquiryId, storageKey, originalFilename, mediaType, fileSize, createdAt)`
- `AttachmentMediaType` (`PDF`, `PNG`, `JPEG`)
- `PendingInquiryAttachment(storageKey, originalFilename, mediaType, fileSize)`
- `InquiryDetail(..., attachments)` and attachment download result

### Approved additions

| Component | Change |
|---|---|
| `UpdateInquiryUseCase` | New inbound port for author-authorized title/content/attachment update. |
| `UpdateInquiryCommand` | Inquiry ID, actor member ID, title, content, validated new pending attachments, and deletion attachment IDs; no Spring/Path types. |
| `InquiryAttachmentRepository` | Add metadata deletion by verified inquiry/attachment IDs as part of the existing DB transaction boundary. |
| `InquiryAttachmentQueryRepository` | Add inquiry attachment aggregate/list query needed to calculate retained count and size; query must return only metadata. |
| `InquiryRepository` / query port | Add author ownership lookup/update capability without exposing persistence types. |
| `InquiryAttachmentStorage` | Use existing delete and compensation operations; no user filename path resolution. |
| `InquiryForm` | Add edit binding fields and attachment input while retaining validation errors and safe redisplay state. |
| `InquirySummary` / `InquiryQueryRepository` | Add list-only `hasAttachments` boolean and derive it in the existing paginated query through `EXISTS` or an equivalent aggregate, never through per-row attachment reads. |

No new attachment table is required for this enhancement. Migration scope is limited to a **data-compatibility migration/test only if the existing database has an enforced 3-file business constraint or data violating the new aggregate assumptions**. The current `inquiry_attachments` schema remains valid because it models each attachment independently. Existing rows remain valid; no storage-key or MIME data migration is required.

### Inquiry-list attachment indicator (planned)

`InquirySummary`와 목록 query result에는 `hasAttachments` boolean만 추가한다. MyBatis 목록 SQL은 `inquiry_attachments`에 대한 상관 `EXISTS` 서브쿼리 또는 동등한 한 번의 `LEFT JOIN`/집계 방식을 사용해 각 문의의 첨부 존재 여부를 계산한다. 페이지 결과를 받은 뒤 행마다 attachment repository/query를 호출하는 방식은 금지한다.

목록 템플릿은 `hasAttachments=true`인 행에만 텍스트로도 보완된 단순 첨부 아이콘/배지를 표시한다. 표시 요소는 보이는 아이콘과 별개로 스크린 리더가 읽을 수 있는 `첨부파일` 접근 가능한 이름을 가진다(예: 숨김 텍스트 또는 동등한 ARIA 이름). `false`인 행에는 표시·숨김 텍스트·빈 placeholder를 렌더링하지 않는다. 목록이 비어 있으면 기존 빈 상태와 페이지네이션만 표시한다. 이 UI는 다운로드 링크나 attachment ID를 만들지 않으며, storage key·경로·기타 파일 metadata를 출력하지 않는다.

## Persistence and Transaction Design

The existing `inquiry_attachments` schema (PK, FK to inquiry, unique storage key, filename, validated MIME, positive size, created timestamp, inquiry index) remains the source of truth. The max-five and total-size rules are cross-row business invariants, so the service must query current attachments and enforce them under the update transaction; they are not assumed to be guaranteed by a per-row database constraint.

For concurrent edits, implementation should serialize or protect the inquiry/attachment aggregate sufficiently to prevent two simultaneous valid requests from exceeding count/total size. The concrete MyBatis approach must be tested (for example, locking the target inquiry row during the write workflow) without breaking the project’s hexagonal ports. A migration test must demonstrate that Liquibase remains idempotent and that pre-existing attachments remain downloadable after deployment.

## UI Design

- `inquiry/list.html` renders the attachment indicator only for `hasAttachments` rows. It uses an unobtrusive icon/badge plus `첨부파일` screen-reader text or an equivalent accessible name; neither an empty indicator nor hidden attachment text appears for rows without attachments or in the empty state.
- `inquiry/detail.html` shows an Edit action only when the current session member is the inquiry author; the server remains authoritative.
- `inquiry/form.html` or a dedicated edit template displays existing files with an individual removal control bound to `deleteAttachmentIds`, plus an optional multi-file add input.
- Guidance must state allowed types, file maximum 10 MiB, final maximum 5 files, and final aggregate maximum 20 MiB.
- Only attachment guidance and the attachment-file-list UI use `font-size: 12px`; this value is within the accessible approved target. No global, form-wide, or unrelated inquiry typography change is permitted.
- HTML `accept` is `.pdf,.png,.jpg,.jpeg` only as UX assistance. The UI must not present the browser-selected file count/size as authoritative.

## Error Handling

| Condition | Result |
|---|---|
| Unauthenticated edit request | Existing login redirect behavior |
| Authenticated non-author edit request | HTTP 403; no file or DB mutation |
| CSRF failure on edit | HTTP 403; no file or DB mutation |
| Invalid title/content/new file/deletion ID | Edit form with safe field/global error; no mutation |
| Final count over 5 or final size over 20 MiB | Edit form with attachment error; no mutation |
| File over 10 MiB or invalid type/name/signature | Edit form with safe attachment error; no mutation |
| Staging/final storage or DB error | Generic failure; compensate new files; preserve old state; security log |
| Final deletion I/O failure after DB commit | Do not expose path; security log and cleanup reconciliation |
| Download metadata/ownership/file absence | Existing generic HTTP 404 |

## Correctness Properties

### Property 1: Page clamping
For any requested page number, the effective page is within 1 through total pages. **Validates: Requirements 1.3**

### Property 2: View-count increment
For any detail request, stored and displayed view count increase by exactly one. **Validates: Requirements 3.2, 3.3**

### Property 3: Creation field validation
For any create input with blank/over-limit title or content, creation is rejected. **Validates: Requirements 2.2, 2.3**

### Property 4: Attachment type validation
For any upload candidate, only PDF/PNG/JPEG with matching extension, declared MIME, and signature is accepted. **Validates: Requirements 5.4, 5.15**

### Property 5: Edit aggregate invariants
For any valid edit candidate, the persisted attachment set contains at most 5 attachments and has total size at most 20 MiB; otherwise no inquiry, metadata, or newly staged/final file change persists. **Validates: Requirements 5.3, 5.14, 5.18**

### Property 6: Ownership and retention isolation
For any edit request, a non-author cannot change inquiry content or attachment metadata/files, and any existing attachment not explicitly selected for deletion remains associated with the inquiry. **Validates: Requirements 5.12, 5.13, 5.16**

### Property 7: Storage-key and download isolation
For any attachment/inquiry ID combination, paths are private-root-contained UUID storage keys only and a non-member attachment relation never opens a stream. **Validates: Requirements 5.6, 5.8, 5.10**

### Property 8: List attachment-presence isolation
For any inquiry list result, `hasAttachments` is true exactly when the inquiry has at least one attached metadata row; the list projection contains no attachment identifier, storage key, path, or download target. **Validates: Requirements 6.1, 6.4, 6.5**

## Testing Strategy

The existing create/download unit, controller, storage, migration, and boundary tests remain required regression coverage. New implementation must add:

| Layer | Required coverage |
|---|---|
| Application service | Owner success, non-owner denial, retain-by-default, individual deletion, add-and-delete same edit, count 5/6 boundary, retained+new total 20 MiB boundary, invalid deletion ownership, compensation. |
| Validator | Per-file 10 MiB, allowed PDF/PNG/JPEG signatures, combined new request limit, filename validation; retain existing tests. |
| Controller | Author edit GET/POST, CSRF failure, non-author 403, field/attachment errors redisplayed, correct multipart inputs, 12px classes scoped only to guidance/list. |
| Persistence/integration | Transactional title/content/metadata update; lock/concurrency behavior; existing rows retained; any required compatibility migration is idempotent. |
| Storage/integration | New-file compensation, post-commit delete failure logging/reconciliation, no deletion of retained files, no path exposure. |
| Inquiry-list attachment indicator | A single paginated list query derives `hasAttachments` without per-row queries; unit/persistence tests cover true and false values, no attachments, and an empty list; template/controller tests cover visible indicator plus `첨부파일` accessible name only when true and no attachment ID/storage key/path output. |
| Property-based tests | Properties 5 and 6 using constrained combinations of retained files, new valid files, deletion sets, counts, sizes, and owner IDs; Property 8 with inquiries that have zero or one-or-more metadata rows and no attachment metadata in the list projection. |

## Security

Existing authentication, session author assignment, CSRF, private storage, UUID keys, root containment, MIME/signature verification, streaming, `nosniff`, and no-store controls remain unchanged. The enhancement adds server-side author ownership enforcement for all edit state changes. Client-provided IDs, file lists, `accept`, sizes, and UI visibility are untrusted inputs and must never authorize deletion or bypass aggregate limits.

## Document Relations

| Document | Scope |
|---|---|
| [`member-auth-baseline`](../member-auth-baseline/) | Initial authentication snapshot |
| [`member-access-management`](../member-access-management/) | Role, status, and access management |
| **This spec** | Inquiry lifecycle, attachments, and approved author edit enhancement |

## Implemented edit extension and validation status

The author-only edit extension described in this document is implemented. `GET /inquiries/{id}/edit` renders the author’s title, content, and existing attachment list; multipart `POST /inquiries/{id}/edit` accepts title/content changes, optional new `attachments`, and zero or more `deleteAttachmentIds`. Existing attachments are retained unless their IDs are selected for deletion. The controller and application service both enforce author ownership; the existing authentication and CSRF layers protect the request.

New uploads are restricted to PDF, PNG, and JPEG only after extension, declared MIME, and signature checks. Each new file is limited to 10 MiB. The server calculates the retained set after requested deletions and rejects a final set over 5 files or 20 MiB; it does not trust the HTML `accept` attribute or browser selection. Attachment guidance and the existing-attachment list are the only inquiry UI selectors set to `12px`.

The update write workflow locks the inquiry aggregate, validates ownership and final limits, stores new UUID-keyed private files, then updates inquiry content and attachment metadata in the database transaction. On validation, storage, or database failure it compensates newly stored files and preserves existing metadata/files. It removes selected old files only after commit; a post-commit removal failure is logged for orphan reconciliation without exposing a path. No edit-specific Liquibase changeSet was required: the existing per-attachment `inquiry_attachments` table, foreign key, UUID storage-key uniqueness, positive-size constraint, and inquiry index remain compatible because the 5-file/20-MiB rules are enforced as aggregate service invariants. Existing metadata, storage keys, and downloads are preserved.

> **Unvalidated scope:** At the user’s request, tasks 9.7.8 and 9.7.9 were skipped. Therefore the planned unit/property, controller/integration/migration regression coverage has not been run or validated for this extension, and this documentation does not claim a passing test suite or build.
