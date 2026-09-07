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
