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
