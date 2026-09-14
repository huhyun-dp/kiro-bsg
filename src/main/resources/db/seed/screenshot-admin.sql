-- 화면 캡처(screenshot) 프로파일 전용 시드.
-- 권한 관리(ADMIN 전용) 화면을 캡처하기 위해, 로그인 가능한 ADMIN 계정을 미리 심는다.
-- 비밀번호 평문: capture1234 (BCrypt cost 4 해시, screenshot 프로파일 bcrypt-strength=4 와 일치)
-- 이 파일은 application-screenshot.yml 에서만 로드되며 운영/기본 시드에는 영향이 없다.

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at, role, status, version)
SELECT 'capture-admin@bsg-demo.local',
       '$2a$04$D02cpEo3AqAUQBYs176vMeJKfB3FfahGeAvnv5.At/jNHMooWY3SO',
       '캡처관리자', '01099998888', CURRENT_TIMESTAMP(6), NULL, 'ADMIN', 'ACTIVE', 0
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'capture-admin@bsg-demo.local');
