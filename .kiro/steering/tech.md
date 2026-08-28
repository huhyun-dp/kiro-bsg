# Tech Stack

## 런타임 환경

| 항목 | 버전/내용 |
|------|-----------|
| Java | 17 |
| Spring Boot | 3.0.5 |
| 빌드 도구 | Maven (mvnw wrapper 포함) |
| 애플리케이션 이름 | `session-auth` |

## 주요 의존성

### 웹 / 뷰
- **spring-boot-starter-web** — Spring MVC
- **spring-boot-starter-thymeleaf** — 서버사이드 HTML 템플릿 (캐시 환경변수 `THYMELEAF_CACHE`)
- **spring-boot-starter-validation** — Bean Validation (Jakarta Validation)
- **TOAST UI Grid** — 회원 목록 테이블 (CDN 방식)
- **Vanilla JS** — 별도 프론트엔드 프레임워크 없음

### 데이터 접근
- **MyBatis** (`mybatis-spring-boot-starter:3.0.1`) — SQL 매퍼, XML 방식
  - 매퍼 위치: `classpath:/mapper/*.xml`
  - underscore → camelCase 자동 변환 (`map-underscore-to-camel-case: true`)
- **MySQL** (`mysql-connector-j`, runtime scope) — 운영 데이터베이스
- **H2** (테스트 전용)
- **HikariCP** — 커넥션 풀 (max 10, min-idle 2, timeout 3초)

### 보안
- **spring-security-crypto** — BCrypt 해싱 전용 (Spring Security 전체 미사용)
  - BCrypt strength 기본값: `12` (환경변수 `BCRYPT_STRENGTH`로 오버라이드)

## 데이터베이스

### 스키마

```sql
CREATE TABLE IF NOT EXISTS members (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,   -- BCrypt 고정 60자
    name          VARCHAR(30) NOT NULL,
    phone_number  VARCHAR(11) NULL,       -- 숫자만 저장 (010XXXXXXXX)
    created_at    DATETIME(6) NOT NULL,   -- 마이크로초 정밀도
    last_login_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email)
);
```

- `schema.sql`은 앱 시작 시 항상 실행 (`spring.sql.init.mode: always`)
- Seed 데이터: `SEED_DEMO_MEMBERS` 환경변수로 제어 (기본 `true`)

## 환경변수 목록

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_URL` | `jdbc:mysql://localhost:3306/lx_auth?...` | 데이터베이스 URL |
| `DB_USERNAME` | `lx_app` | DB 사용자명 |
| `DB_PASSWORD` | `lx_app_password` | DB 비밀번호 |
| `DB_POOL_SIZE` | `10` | HikariCP 최대 커넥션 수 |
| `BCRYPT_STRENGTH` | `12` | BCrypt 해싱 강도 |
| `SEED_DEMO_MEMBERS` | `true` | 데모 멤버 시드 데이터 실행 여부 |
| `SERVER_PORT` | `8080` | 서버 포트 |
| `SESSION_COOKIE_SECURE` | `false` | 세션 쿠키 Secure 플래그 (HTTPS 환경에서 `true` 설정 필요) |
| `THYMELEAF_CACHE` | `true` | Thymeleaf 캐시 활성화 여부 |

## 빌드 및 실행

```bash
# 빌드
./mvnw clean package

# 실행
./mvnw spring-boot:run

# Docker Compose (compose.yaml 제공)
docker compose up
```

## 세션 설정

- 쿠키명: `BSG_SESSION`
- 유효 시간: 30분
- `HttpOnly: true`, `SameSite: Lax`
- Secure 플래그: 환경변수 `SESSION_COOKIE_SECURE` (운영 환경에서 반드시 `true`)
- 추적 방식: Cookie only (`tracking-modes: cookie`)
- `forward-headers-strategy: framework` — 리버스 프록시 헤더 처리

## 코드 컨벤션

- Java record를 도메인 모델과 DTO에 적극 활용
- 애플리케이션 서비스에 Spring 어노테이션 사용 금지 — `@Configuration` 클래스에서 수동 빈 등록
- MyBatis 매퍼는 XML 파일만 사용 (어노테이션 매퍼 혼용 금지)
- 모든 예외는 `application/exception` 패키지에 정의하고 도메인 의미를 담은 이름 사용
