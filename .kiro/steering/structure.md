# Project Structure

## 아키텍처 패턴

**헥사고날 아키텍처 (Hexagonal Architecture / Ports & Adapters)** 를 적용합니다.

- **도메인(Domain)**: 외부 의존성 없는 순수 Java. 비즈니스 규칙만 포함
- **애플리케이션(Application)**: Use Case 인터페이스(포트)와 서비스 구현체. Spring 어노테이션 없음
- **어댑터(Adapter)**: 외부 세계(HTTP, DB, 암호화)와의 연결. Spring/MyBatis 어노테이션 허용
- **CQRS 분리**: 쓰기 (AuthenticationService + MemberRepository) / 읽기 (MemberQueryService + MemberQueryRepository) 분리
- **권한 관리(CQRS)**: 쓰기 (AccessManagementService + AccessMemberRepository) / 읽기 (AccessMemberQueryRepository) 분리, 권한 변경과 감사 로그 저장은 `TransactionRunner` 포트로 동일 트랜잭션 보장

## 패키지 구조

```
com.lxpantos.auth
├── KiroDemoApplication.java
│
├── domain/
│   └── member/
│       ├── Member.java                        # 도메인 엔티티 (Java record, role/status/version/roleUpdatedAt 포함)
│       ├── MemberRole.java                    # 역할 enum (ADMIN/OPERATOR/VIEWER)
│       └── MemberStatus.java                  # 계정 상태 enum (ACTIVE/SUSPENDED)
│
├── application/
│   ├── port/
│   │   ├── in/                                # 인바운드 포트 (Use Case 인터페이스 + Command/Result)
│   │   │   ├── LoginUseCase.java
│   │   │   ├── LoginCommand.java
│   │   │   ├── RegisterMemberUseCase.java
│   │   │   ├── RegisterMemberCommand.java
│   │   │   ├── MemberQueryUseCase.java
│   │   │   ├── MemberSummary.java
│   │   │   ├── AuthenticatedMember.java        # role 포함
│   │   │   ├── AccessManagementUseCase.java    # 권한 관리 인바운드 포트
│   │   │   ├── MemberAccessLookupUseCase.java  # 세션 검증용 역할/상태 조회 포트
│   │   │   ├── AccessMemberSearchQuery.java / AccessMemberSummary.java / AccessMemberPage.java
│   │   │   ├── ChangeMemberAccessCommand.java  # 역할/상태 변경 Command
│   │   │   ├── AccessAuditLogEntry.java / AccessAuditLogPage.java
│   │   │   └── CurrentMemberAccess.java
│   │   └── out/                               # 아웃바운드 포트 (Repository/Service 인터페이스)
│   │       ├── MemberRepository.java
│   │       ├── MemberQueryRepository.java
│   │       ├── MemberQueryResult.java
│   │       ├── PasswordHasher.java
│   │       ├── AccessMemberRepository.java     # 권한 쓰기 포트(낙관적 잠금 update, 감사 로그 저장)
│   │       ├── AccessMemberQueryRepository.java# 권한 읽기 포트(검색/카운트/감사 로그)
│   │       ├── AccessMemberView.java / AuditLogView.java / AuditLogEntry.java / CurrentMemberAccessView.java
│   │       └── TransactionRunner.java          # 트랜잭션 경계 포트
│   ├── service/                               # Use Case 구현체 (Spring 어노테이션 없음)
│   │   ├── AuthenticationService.java         # LoginUseCase + RegisterMemberUseCase 구현 (SUSPENDED 로그인 차단)
│   │   ├── MemberQueryService.java            # MemberQueryUseCase 구현
│   │   └── AccessManagementService.java       # AccessManagementUseCase + MemberAccessLookupUseCase 구현
│   └── exception/
│       ├── DuplicateEmailException.java
│       ├── InvalidCredentialsException.java
│       ├── SuspendedMemberException.java       # 정지 회원 로그인
│       ├── MemberNotFoundException.java        # 404
│       ├── AccessRuleViolationException.java   # 400 (규칙 위반/입력 오류)
│       └── OptimisticLockConflictException.java# 409
│
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── AuthController.java            # GET/POST /login, /signup, /logout
│   │       ├── HomeController.java            # GET / → 리다이렉트, GET /members
│   │       ├── MemberQueryApiController.java  # GET /api/members
│   │       ├── AccessManagementPageController.java     # GET /admin/access (권한 관리 화면)
│   │       ├── AccessManagementApiController.java      # GET/PUT /api/admin/members[...]
│   │       ├── AccessManagementApiExceptionHandler.java# 관리자 API 예외 → 상태/JSON 매핑
│   │       ├── ApiErrorResponse.java          # 일관된 오류 응답 record
│   │       ├── form/
│   │       │   ├── LoginForm.java             # 로그인 폼 DTO (Bean Validation)
│   │       │   ├── SignUpForm.java            # 회원가입 폼 DTO (Bean Validation)
│   │       │   └── ChangeAccessRequest.java   # 권한 변경 API 요청 DTO (Bean Validation)
│   │       ├── session/
│   │       │   ├── SessionKeys.java           # 세션 attribute 키 상수
│   │       │   └── SessionMember.java         # 세션 저장용 record (role 포함, Serializable)
│   │       └── security/
│   │           ├── AuthenticationInterceptor.java     # /members/** 인증 + 정지 회원 차단
│   │           ├── ApiAuthenticationInterceptor.java  # /api/** 인증 + 정지 회원 차단 (401)
│   │           ├── AdminPageAuthorizationInterceptor.java # /admin/** ADMIN 권한 (403)
│   │           ├── AdminApiAuthorizationInterceptor.java  # /api/admin/** ADMIN 권한 (401/403)
│   │           └── CsrfTokenFilter.java               # 커스텀 CSRF 필터
│   └── out/
│       ├── persistence/
│       │   ├── SpringTransactionRunner.java   # TransactionRunner 구현(TransactionTemplate)
│       │   └── mybatis/
│       │       ├── MemberMapper.java              # MyBatis 매퍼 인터페이스 (쓰기용)
│       │       ├── MemberQueryMapper.java         # MyBatis 매퍼 인터페이스 (읽기용)
│       │       ├── MemberPersistenceModel.java    # 퍼시스턴스 모델 (role/status/version/roleUpdatedAt 포함)
│       │       ├── MyBatisMemberRepository.java   # MemberRepository 구현체
│       │       ├── MyBatisMemberQueryRepository.java # MemberQueryRepository 구현체
│       │       ├── AccessMemberMapper.java        # 권한 쓰기 매퍼(낙관적 잠금 update, 감사 로그 insert)
│       │       ├── AccessMemberQueryMapper.java   # 권한 읽기 매퍼(검색/카운트/감사 로그)
│       │       ├── AuditLogPersistenceModel.java  # 감사 로그 퍼시스턴스 모델
│       │       ├── MyBatisAccessMemberRepository.java
│       │       └── MyBatisAccessMemberQueryRepository.java
│       └── security/
│           └── BCryptPasswordHasher.java      # PasswordHasher 구현체
│
└── config/
    ├── ApplicationConfiguration.java          # Clock 빈, AuthenticationService 빈 등록
    ├── WebConfiguration.java                  # AuthenticationInterceptor 등록 (/members/**)
    ├── MemberQueryConfiguration.java          # ApiAuthenticationInterceptor 등록 (/api/**)
    │                                          # MemberQueryService 빈 등록
    ├── AccessManagementConfiguration.java     # AccessManagementService, TransactionTemplate 빈 등록
    ├── AccessManagementWebConfiguration.java  # 관리자 인터셉터 등록 (/admin/**, /api/admin/**)
    └── BootstrapAdminInitializer.java         # BOOTSTRAP_ADMIN_EMAIL 초기 관리자 승격
```

## 리소스 구조

```
src/main/resources/
├── application.yml                   # 애플리케이션 설정
├── db/
│   ├── changelog/
│   │   └── db.changelog-master.yaml  # Liquibase 스키마 생성·증분 변경 이력
│   └── seed/
│       └── member-seed-true.sql       # 데모 시드 데이터
├── mapper/
│   ├── MemberMapper.xml              # 쓰기 쿼리 (insert, update, select; role/status/version 포함)
│   ├── MemberQueryMapper.xml         # 검색 쿼리 (keyword LIKE 검색)
│   ├── AccessMemberMapper.xml        # 권한 쓰기 쿼리 (낙관적 잠금 update, 감사 로그 insert)
│   └── AccessMemberQueryMapper.xml   # 권한 읽기 쿼리 (검색/카운트/감사 로그, 페이지네이션)
├── static/
│   ├── css/app.css                   # 권한 관리 화면/모달/페이지네이션 스타일 포함
│   └── js/
│       ├── signup.js                 # 휴대폰 번호 자동 포맷팅
│       ├── members.js                # TOAST UI Grid 초기화 + API 호출
│       └── access.js                 # 권한 관리 그리드/필터/페이지네이션/변경 모달/감사 로그
└── templates/
    ├── auth/
    │   ├── login.html                # 로그인 페이지
    │   └── signup.html               # 회원가입 페이지
    ├── fragments/
    │   └── layout.html               # 공통 헤더/사이드바 Thymeleaf fragment
    ├── admin/
    │   └── access.html               # 권한 관리 페이지
    ├── members.html                  # 회원 목록 페이지
    └── error.html                    # 에러 페이지
```

## 레이어 간 의존성 규칙

```
adapter/in  →  application/port/in  ←  application/service
adapter/out ←  application/port/out  ←  application/service
                                              ↑
                                         domain (의존성 없음)
```

- `domain`은 어떤 레이어에도 의존하지 않음
- `application/service`는 `domain`과 `application/port`에만 의존
- `adapter`는 `application/port`를 통해서만 서비스와 통신
- `config`는 모든 레이어의 빈을 조립하는 역할

## 새 기능 추가 시 체크리스트

1. **도메인 모델**: `domain/` 에 순수 Java record 또는 클래스 추가
2. **인바운드 포트**: `application/port/in/` 에 Use Case 인터페이스 + Command/Result record 추가
3. **아웃바운드 포트**: `application/port/out/` 에 Repository/Service 인터페이스 추가
4. **서비스**: `application/service/` 에 Use Case 구현체 추가 (Spring 어노테이션 없이)
5. **퍼시스턴스 어댑터**: `adapter/out/persistence/mybatis/` 에 Mapper 인터페이스 + 구현체 + XML 추가
6. **웹 어댑터**: `adapter/in/web/` 에 Controller + Form DTO 추가
7. **빈 등록**: `config/` 의 `@Configuration` 클래스에서 서비스 빈 수동 등록
   - 인터셉터를 등록하는 `WebMvcConfigurer` 설정 클래스에는 인터셉터가 의존하는 서비스 `@Bean` 을 함께 두지 않는다(순환 참조 방지). 빈 정의와 인터셉터 등록을 별도 `@Configuration` 으로 분리한다.
8. **인터셉터 등록**: 새 경로 보호가 필요하면 `WebConfiguration` 또는 `MemberQueryConfiguration` 에 인터셉터 추가

## Spec 네이밍 및 구조 컨벤션

`.kiro/specs/` 아래 Spec 문서를 생성·변경할 때 아래 규칙을 따른다. 이 규칙은 향후 모든 Spec 작업에 적용된다.

### 폴더 네이밍

- **형식**: `kebab-case` — 영문 소문자, 숫자, 하이픈(`-`)만 사용한다. 대문자·공백·언더스코어(`_`)·한글·특수문자 금지.
- **의미**: 기능/도메인을 나타내는 명사 중심으로 짓는다. 2~4단어를 권장한다.
- **도메인 접두**: 특정 도메인에 속한 기능은 `<도메인>-<기능>` 형태로 접두어를 두어 정렬 시 응집도를 높이는 것을 권장한다(예: `member-access`). 단, 이미 의미가 분명하고 독립적인 기능은 단일 명사도 허용한다(예: `inquiry`).
- **예약 성격**: AS-IS(현행 분석) 스냅샷 문서는 `-analysis` 접미어로 성격을 드러낸다(예: `as-is-system-analysis`).
- 한 기능(독립적으로 설계·릴리스 가능한 단위) = 한 폴더. 성격이 다른 기능을 한 폴더에 섞지 않는다.

### 폴더 내부 파일 구조 (필수)

각 Spec 폴더는 다음 파일을 포함한다. 파일명은 고정이며 변경하지 않는다.

```
.kiro/specs/<spec-name>/
├── .config.kiro        # {"specId": "<uuid>", "workflowType": "...", "specType": "..."} (한 줄 JSON)
├── requirements.md     # 요구사항 (EARS 스타일 Acceptance Criteria, ✅/🔶/❓ 표기)
├── design.md           # 설계 (Overview/Architecture/Components/Data Models/Error Handling/Properties/Testing)
├── tasks.md            # 구현·확인 체크리스트 ([x] 완료 / [ ] 확인·향후)
└── screenshots/        # (선택) 화면 캡처
```

- `.config.kiro`의 `specId`는 Spec마다 고유한 UUID를 사용한다.
- `tasks.md`의 제목은 `# Implementation Plan: <spec-name>` 형식을 사용한다.

### Spec 종류와 갱신 원칙

- **living spec** (예: `access-management`, `inquiry`): 해당 기능이 바뀌면 계속 최신화한다.
- **snapshot spec** (예: `as-is-system-analysis`): 특정 시점 기록이므로 본문을 편집하지 않고 보존한다. 후속 변경은 새 living spec으로 분리하고, 스냅샷에는 "해소됨 → 대상 spec 참조" 링크만 남긴다.

### 어떤 Spec에 작성할지 판단 기준

- 기존 기능의 확장/수정 → 해당 기능의 living spec을 갱신한다(불필요한 신규 Spec 남발 금지).
- 독립적으로 설계·릴리스할 만큼 큰 새 기능 → 새 Spec 폴더를 만든다.
- 성격이 완전히 다른 내용을 기존 Spec에 억지로 끼워 넣지 않는다.
- 어떤 경우든 always steering(`product.md`/`structure.md`/`tech.md`)은 최신 구현을 반영하도록 함께 갱신한다.

### 상호 참조

- Spec 간 링크는 상대 경로를 사용한다(예: `../access-management/requirements.md`).
- 각 living spec 문서 하단에 다른 Spec과의 관계(범위 구분)를 표로 명시한다.

### 현재 Spec 목록 (컨벤션 부합)

| 폴더 | 종류 | 범위 |
|------|------|------|
| `as-is-system-analysis` | snapshot | 권한 구분 이전 초기 시스템(회원가입·로그인·회원 조회) |
| `access-management` | living | 역할·상태·권한 관리·감사 로그 |
| `inquiry` | living | 문의 작성·목록·상세(조회수) |

> 위 3개 폴더는 모두 kebab-case 규칙에 부합하므로 폴더명을 변경하지 않는다. 향후 신규 Spec부터 본 컨벤션을 그대로 적용한다.
