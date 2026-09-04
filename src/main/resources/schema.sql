CREATE TABLE IF NOT EXISTS members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    name VARCHAR(30) NOT NULL,
    phone_number VARCHAR(11) NULL,
    created_at DATETIME(6) NOT NULL,
    last_login_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email)
);

-- 권한 관리 기능을 위한 안전한(추가 전용) 스키마 확장.
-- 기존 데이터는 삭제/초기화하지 않으며 기본값으로 채워진다.
-- H2(MySQL 호환 모드)와 MySQL 8 모두 ADD COLUMN IF NOT EXISTS 를 지원한다.
ALTER TABLE members ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'VIEWER';
ALTER TABLE members ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE members ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE members ADD COLUMN IF NOT EXISTS role_updated_at DATETIME(6) NULL;

-- 권한/상태 변경 감사 로그. 변경과 동일 트랜잭션에서 기록된다.
CREATE TABLE IF NOT EXISTS member_access_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_member_id BIGINT NOT NULL,
    actor_member_id BIGINT NOT NULL,
    before_role VARCHAR(20) NOT NULL,
    before_status VARCHAR(20) NOT NULL,
    after_role VARCHAR(20) NOT NULL,
    after_status VARCHAR(20) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    request_ip VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_audit_target_created
    ON member_access_audit_log (target_member_id, created_at DESC, id DESC);
