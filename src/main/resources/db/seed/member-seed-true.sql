-- 회원관리 화면 Test용 초기 회원 100명입니다.
-- 운영 환경에서는 SEED_DEMO_MEMBERS=false로 비활성화합니다.

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member001@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원001', '01090000001', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member001@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member002@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원002', '01090000002', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member002@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member003@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원003', '01090000003', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member003@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member004@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원004', '01090000004', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member004@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member005@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원005', '01090000005', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member005@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member006@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원006', '01090000006', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member006@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member007@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원007', '01090000007', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member007@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member008@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원008', '01090000008', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member008@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member009@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원009', '01090000009', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member009@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member010@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원010', '01090000010', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member010@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member011@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원011', '01090000011', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member011@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member012@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원012', '01090000012', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member012@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member013@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원013', '01090000013', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member013@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member014@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원014', '01090000014', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member014@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member015@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원015', '01090000015', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member015@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member016@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원016', '01090000016', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member016@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member017@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원017', '01090000017', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member017@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member018@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원018', '01090000018', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member018@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member019@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원019', '01090000019', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member019@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member020@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원020', '01090000020', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member020@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member021@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원021', '01090000021', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member021@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member022@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원022', '01090000022', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member022@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member023@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원023', '01090000023', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member023@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member024@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원024', '01090000024', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member024@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member025@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원025', '01090000025', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member025@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member026@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원026', '01090000026', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member026@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member027@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원027', '01090000027', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member027@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member028@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원028', '01090000028', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member028@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member029@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원029', '01090000029', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member029@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member030@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원030', '01090000030', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member030@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member031@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원031', '01090000031', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member031@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member032@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원032', '01090000032', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member032@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member033@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원033', '01090000033', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member033@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member034@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원034', '01090000034', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member034@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member035@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원035', '01090000035', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member035@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member036@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원036', '01090000036', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member036@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member037@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원037', '01090000037', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member037@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member038@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원038', '01090000038', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member038@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member039@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원039', '01090000039', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member039@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member040@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원040', '01090000040', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member040@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member041@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원041', '01090000041', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member041@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member042@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원042', '01090000042', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member042@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member043@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원043', '01090000043', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member043@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member044@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원044', '01090000044', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member044@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member045@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원045', '01090000045', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member045@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member046@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원046', '01090000046', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member046@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member047@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원047', '01090000047', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member047@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member048@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원048', '01090000048', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member048@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member049@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원049', '01090000049', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member049@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member050@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원050', '01090000050', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member050@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member051@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원051', '01090000051', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member051@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member052@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원052', '01090000052', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member052@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member053@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원053', '01090000053', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member053@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member054@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원054', '01090000054', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member054@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member055@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원055', '01090000055', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member055@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member056@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원056', '01090000056', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member056@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member057@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원057', '01090000057', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member057@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member058@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원058', '01090000058', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member058@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member059@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원059', '01090000059', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member059@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member060@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원060', '01090000060', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member060@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member061@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원061', '01090000061', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member061@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member062@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원062', '01090000062', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member062@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member063@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원063', '01090000063', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member063@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member064@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원064', '01090000064', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member064@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member065@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원065', '01090000065', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member065@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member066@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원066', '01090000066', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member066@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member067@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원067', '01090000067', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member067@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member068@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원068', '01090000068', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member068@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member069@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원069', '01090000069', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member069@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member070@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원070', '01090000070', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member070@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member071@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원071', '01090000071', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member071@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member072@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원072', '01090000072', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member072@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member073@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원073', '01090000073', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member073@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member074@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원074', '01090000074', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member074@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member075@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원075', '01090000075', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member075@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member076@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원076', '01090000076', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member076@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member077@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원077', '01090000077', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member077@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member078@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원078', '01090000078', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member078@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member079@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원079', '01090000079', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member079@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member080@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원080', '01090000080', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member080@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member081@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원081', '01090000081', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member081@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member082@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원082', '01090000082', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member082@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member083@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원083', '01090000083', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member083@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member084@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원084', '01090000084', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member084@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member085@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원085', '01090000085', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member085@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member086@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원086', '01090000086', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member086@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member087@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원087', '01090000087', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member087@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member088@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원088', '01090000088', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member088@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member089@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원089', '01090000089', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member089@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member090@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원090', '01090000090', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member090@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member091@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원091', '01090000091', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member091@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member092@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원092', '01090000092', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member092@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member093@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원093', '01090000093', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member093@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member094@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원094', '01090000094', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member094@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member095@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원095', '01090000095', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member095@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member096@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원096', '01090000096', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member096@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member097@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원097', '01090000097', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member097@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member098@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원098', '01090000098', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member098@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member099@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원099', '01090000099', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member099@bsg-demo.local');

INSERT INTO members (email, password_hash, name, phone_number, created_at, last_login_at)
SELECT 'member100@bsg-demo.local', '$2a$12$xbEInUGLUCGzWBexO07sJ.yIjcax/fY0JNJRzra7aiJ0bt27M2H7S', '테스트회원100', '01090000100', CURRENT_TIMESTAMP(6), NULL
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = 'member100@bsg-demo.local');

