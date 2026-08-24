# Kiro BSG

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.0.5, Spring MVC |
| View | Thymeleaf, HTML5, CSS3 |
| Validation | Jakarta Bean Validation |
| Persistence | MyBatis Spring Boot 3.0.1 |
| Database | MySQL 8.4.10 |
| Security | BCrypt, Session Authentication, CSRF |
| Build | Maven Wrapper 3.9.11 |
| Infrastructure | Docker Compose |

## 구조

```text
com.lxpantos.auth
├─ domain/member                 순수 도메인 모델
├─ application
│  ├─ port/in                   회원가입·로그인 유스케이스
│  ├─ port/out                  저장소·암호화 포트
│  └─ service                   인증 애플리케이션 서비스
├─ adapter
│  ├─ in/web                    Thymeleaf MVC, 세션, CSRF
│  └─ out                       MyBatis 저장소, BCrypt
└─ config                       의존성 조립, MVC 설정
```

## 실행
| Docker 실행 방법을 기준으로 작성합니다.

1. 환경 파일을 준비합니다.

   ```powershell
   Copy-Item .env.example .env
   ```

2. docker demon을 구동한 뒤 아래 명령어로 MySQL을 실행합니다.

   ```powershell
   docker compose up -d
   ```

3. 프로젝트 root 위치에서 아래 명령어를 수행합니다.

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. `http://localhost:8080/signup`에서 가입 후 로그인합니다. 로그인 성공 후 `/members` 회원관리
   화면으로 이동합니다.

## 인증 및 세션 정책

- 비밀번호는 BCrypt(기본 cost 12) 해시만 DB에 저장합니다.
- 로그인 성공 시 세션 ID를 교체해 session fixation을 방지합니다.
- 세션에는 회원 ID, 이메일, 이름만 저장하고 30분 후 만료합니다.
- 세션 쿠키는 `HttpOnly`, `SameSite=Lax`, cookie-only tracking을 사용합니다.
- **운영** HTTPS 환경에서는 `SESSION_COOKIE_SECURE=true`를 반드시 설정합니다.
- 모든 상태 변경 POST 요청은 세션 기반 CSRF 토큰으로 검증합니다.
- 로그아웃은 POST만 허용하고 서버 세션을 즉시 무효화합니다.

## DB

앱 시작 시 [`schema.sql`](src/main/resources/schema.sql)이 실행됩니다.

- 휴대폰 번호는 하이픈을 제거해 저장하고 회원관리 API에서는 `010-****-1234`로 마스킹합니다.
- 가입일시와 마지막 로그인 일시는 `Asia/Seoul` 기준으로 저장합니다.

### 초기 회원 데이터

[`member-seed-true.sql`](src/main/resources/db/seed/member-seed-true.sql)은 애플리케이션 초기화 시
`member001@bsg-demo.local`부터 `member100@bsg-demo.local`까지 회원 100명을 중복 없이 추가합니다.
초기 회원은 원문을 보관하지 않은 무작위 BCrypt 해시를 사용하므로 로그인용 계정이 아니라
회원관리 화면 조회용 데이터입니다.

운영 환경에서는 `SEED_DEMO_MEMBERS=false`로 설정해 초기 회원 생성을 비활성화합니다.

### DB 연결 정보

| 항목 | 기본값 |
| --- | --- |
| Host | `localhost` |
| Port | `3306` |
| User | `lx_app` |
| Password | `lx_app_password` |
| Database | `lx_auth` |

_ex) JDBC URL : `jdbc:mysql://localhost:3306/lx_auth`_
