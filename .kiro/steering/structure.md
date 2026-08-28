# Project Structure

## 아키텍처 패턴

**헥사고날 아키텍처 (Hexagonal Architecture / Ports & Adapters)** 를 적용합니다.

- **도메인(Domain)**: 외부 의존성 없는 순수 Java. 비즈니스 규칙만 포함
- **애플리케이션(Application)**: Use Case 인터페이스(포트)와 서비스 구현체. Spring 어노테이션 없음
- **어댑터(Adapter)**: 외부 세계(HTTP, DB, 암호화)와의 연결. Spring/MyBatis 어노테이션 허용
- **CQRS 분리**: 쓰기 (AuthenticationService + MemberRepository) / 읽기 (MemberQueryService + MemberQueryRepository) 분리

## 패키지 구조

```
com.lxpantos.auth
├── KiroDemoApplication.java
│
├── domain/
│   └── member/
│       └── Member.java                        # 도메인 엔티티 (Java record)
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
│   │   │   └── AuthenticatedMember.java
│   │   └── out/                               # 아웃바운드 포트 (Repository/Service 인터페이스)
│   │       ├── MemberRepository.java
│   │       ├── MemberQueryRepository.java
│   │       ├── MemberQueryResult.java
│   │       └── PasswordHasher.java
│   ├── service/                               # Use Case 구현체 (Spring 어노테이션 없음)
│   │   ├── AuthenticationService.java         # LoginUseCase + RegisterMemberUseCase 구현
│   │   └── MemberQueryService.java            # MemberQueryUseCase 구현
│   └── exception/
│       ├── DuplicateEmailException.java
│       └── InvalidCredentialsException.java
│
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── AuthController.java            # GET/POST /login, /signup, /logout
│   │       ├── HomeController.java            # GET / → 리다이렉트, GET /members
│   │       ├── MemberQueryApiController.java  # GET /api/members
│   │       ├── form/
│   │       │   ├── LoginForm.java             # 로그인 폼 DTO (Bean Validation)
│   │       │   └── SignUpForm.java            # 회원가입 폼 DTO (Bean Validation)
│   │       ├── session/
│   │       │   ├── SessionKeys.java           # 세션 attribute 키 상수
│   │       │   └── SessionMember.java         # 세션 저장용 record (Serializable)
│   │       └── security/
│   │           ├── AuthenticationInterceptor.java     # /members/** 인증 인터셉터
│   │           ├── ApiAuthenticationInterceptor.java  # /api/** 인증 인터셉터 (401 반환)
│   │           └── CsrfTokenFilter.java               # 커스텀 CSRF 필터
│   └── out/
│       ├── persistence/mybatis/
│       │   ├── MemberMapper.java              # MyBatis 매퍼 인터페이스 (쓰기용)
│       │   ├── MemberQueryMapper.java         # MyBatis 매퍼 인터페이스 (읽기용)
│       │   ├── MemberPersistenceModel.java    # 퍼시스턴스 모델 (DB 컬럼 매핑용)
│       │   ├── MyBatisMemberRepository.java   # MemberRepository 구현체
│       │   └── MyBatisMemberQueryRepository.java # MemberQueryRepository 구현체
│       └── security/
│           └── BCryptPasswordHasher.java      # PasswordHasher 구현체
│
└── config/
    ├── ApplicationConfiguration.java          # Clock 빈, AuthenticationService 빈 등록
    ├── WebConfiguration.java                  # AuthenticationInterceptor 등록 (/members/**)
    └── MemberQueryConfiguration.java          # ApiAuthenticationInterceptor 등록 (/api/**)
                                               # MemberQueryService 빈 등록
```

## 리소스 구조

```
src/main/resources/
├── application.yml                   # 애플리케이션 설정
├── schema.sql                        # 테이블 DDL (앱 시작 시 항상 실행)
├── db/seed/
│   └── member-seed-true.sql          # 데모 시드 데이터
├── mapper/
│   ├── MemberMapper.xml              # 쓰기 쿼리 (insert, update, select)
│   └── MemberQueryMapper.xml         # 검색 쿼리 (keyword LIKE 검색)
├── static/
│   ├── css/app.css
│   └── js/
│       ├── signup.js                 # 휴대폰 번호 자동 포맷팅
│       └── members.js                # TOAST UI Grid 초기화 + API 호출
└── templates/
    ├── auth/
    │   ├── login.html                # 로그인 페이지
    │   └── signup.html               # 회원가입 페이지
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
8. **인터셉터 등록**: 새 경로 보호가 필요하면 `WebConfiguration` 또는 `MemberQueryConfiguration` 에 인터셉터 추가
