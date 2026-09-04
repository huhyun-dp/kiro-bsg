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

CREATE TABLE IF NOT EXISTS inquiries (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    member_id  BIGINT NOT NULL,
    title      VARCHAR(20) NOT NULL,
    content    TEXT NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inquiries_member FOREIGN KEY (member_id) REFERENCES members (id)
);
