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
- **Liquibase** — changeSet 기반 DB 스키마 생성 및 기존 DB 마이그레이션
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
    id              BIGINT NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(60) NOT NULL,   -- BCrypt 고정 60자
    name            VARCHAR(30) NOT NULL,
    phone_number    VARCHAR(11) NULL,       -- 숫자만 저장 (010XXXXXXXX)
    created_at      DATETIME(6) NOT NULL,   -- 마이크로초 정밀도
    last_login_at   DATETIME(6) NULL,
    -- 권한 관리 컬럼(Liquibase changeSet에서 추가)
    role            VARCHAR(20) NOT NULL DEFAULT 'VIEWER',   -- ADMIN/OPERATOR/VIEWER
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE/SUSPENDED
    version         BIGINT NOT NULL DEFAULT 0,               -- 낙관적 잠금
    role_updated_at DATETIME(6) NULL,                        -- 권한 최종 변경 일시
    PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email)
);

-- 권한/상태 변경 감사 로그 (변경과 동일 트랜잭션에서 기록)
CREATE TABLE IF NOT EXISTS member_access_audit_log (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    target_member_id BIGINT NOT NULL,
    actor_member_id  BIGINT NOT NULL,
    before_role      VARCHAR(20) NOT NULL,
    before_status    VARCHAR(20) NOT NULL,
    after_role       VARCHAR(20) NOT NULL,
    after_status     VARCHAR(20) NOT NULL,
    reason           VARCHAR(500) NOT NULL,
    request_ip       VARCHAR(45) NULL,
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
```

- Liquibase가 `db/changelog/db.changelog-master.yaml`의 changeSet 이력을 관리
- 기존 테이블·컬럼·인덱스는 precondition으로 감지해 유지하고 누락된 권한 관리 스키마만 적용
- `spring.sql.init`은 Liquibase 마이그레이션 이후 환경별 데모 seed 데이터만 실행
- Seed 데이터: `SEED_DEMO_MEMBERS` 환경변수로 제어 (기본 `true`, 데모 시 `member001`=ADMIN / `member002`=OPERATOR 로 지정)
- 초기 관리자: `BOOTSTRAP_ADMIN_EMAIL` 로 지정한 회원을 앱 시작 시 ADMIN/ACTIVE 로 승격

## 환경변수 목록

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_URL` | `jdbc:mysql://localhost:3306/lx_auth?...` | 데이터베이스 URL |
| `DB_USERNAME` | `lx_app` | DB 사용자명 |
| `DB_PASSWORD` | `lx_app_password` | DB 비밀번호 |
| `DB_POOL_SIZE` | `10` | HikariCP 최대 커넥션 수 |
| `BCRYPT_STRENGTH` | `12` | BCrypt 해싱 강도 |
| `SEED_DEMO_MEMBERS` | `true` | 데모 멤버 시드 데이터 실행 여부 |
| `BOOTSTRAP_ADMIN_EMAIL` | (빈 값) | 앱 시작 시 해당 이메일 회원을 ADMIN/ACTIVE 로 승격(초기 관리자) |
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

## 문의 첨부파일 저장소

- `INQUIRY_ATTACHMENT_STORAGE_PATH`는 필수 절대 경로이며 웹 정적 리소스 밖의 전용 비공개 볼륨이어야 한다. 비어 있거나 상대 경로면 애플리케이션 시작을 중단한다.
- multipart parser는 파일당 10 MiB, 요청당 21 MiB로 제한한다. 애플리케이션은 생성·작성자 편집 모두에서 최종 첨부 5개·20 MiB를 서버 측으로 검증하고, 신규 파일은 PDF/PNG/JPEG의 확장자·선언 MIME·서명을 모두 확인한다.
- 편집은 `GET`/multipart `POST /inquiries/{id}/edit`와 `deleteAttachmentIds`를 사용한다. 기존 첨부는 명시 삭제 전까지 보존하며, 새 파일은 실패 시 보상 삭제하고 선택 삭제 파일은 DB 커밋 후 제거한다. 제거 실패는 경로를 노출하지 않고 orphan reconciliation 로그 대상으로 남긴다.
- 현재 `inquiry_attachments` 스키마는 변경하지 않았다. 5개·20 MiB 제한은 행별 제약이 아니라 애플리케이션의 aggregate invariant이며 기존 metadata·UUID storage key·다운로드 호환성을 보존한다.
- `INQUIRY_ATTACHMENT_CLEANUP_ON_STARTUP`(기본 `false`)을 켜면 24시간보다 오래된 DB 비참조 UUID 파일을 안전하게 정리한다. 운영 환경에서는 저장소 권한·백업·보존 기간을 별도 관리한다.

> 문의 편집의 단위/속성 기반 및 controller/integration/migration 검증 작업(9.7.8, 9.7.9)은 사용자 요청으로 건너뛰었다. 이 문서 갱신은 테스트·빌드 실행 또는 통과를 의미하지 않는다.
