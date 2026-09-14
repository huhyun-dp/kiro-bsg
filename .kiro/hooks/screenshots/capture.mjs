// 신규/수정 화면을 건별 PNG 로 캡처하는 Playwright 스크립트.
// 오케스트레이터(capture-screens.ps1)가 앱을 띄운 뒤 이 스크립트를 호출한다.
//
// 사용법:
//   node capture.mjs <baseUrl> <outDir> <adminEmail> <password>
//
// 표준출력 마지막 줄에 결과 JSON 을 찍는다: {"total":N,"ok":N,"fail":N,"shots":[...]}

import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';

const [baseUrl, outDir, adminEmail, password] = process.argv.slice(2);
if (!baseUrl || !outDir || !adminEmail || !password) {
  console.error('usage: node capture.mjs <baseUrl> <outDir> <adminEmail> <password>');
  process.exit(2);
}

fs.mkdirSync(outDir, { recursive: true });

const shots = [];
let seq = 0;

async function shoot(page, name, description) {
  seq += 1;
  const file = path.join(outDir, `${String(seq).padStart(2, '0')}-${name}.png`);
  try {
    await page.screenshot({ path: file, fullPage: true });
    shots.push({ name, description, file, ok: true });
    console.log(`[OK]   ${path.basename(file)} — ${description}`);
  } catch (e) {
    shots.push({ name, description, file, ok: false, error: String(e) });
    console.log(`[FAIL] ${path.basename(file)} — ${description}: ${e}`);
  }
}

const run = async () => {
  const browser = await chromium.launch();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // 1) 로그인 화면 (미인증)
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await shoot(page, 'login', '로그인 화면');

  // 2) 회원가입 화면 (입력 예시를 채운 상태로 캡처, 실제 제출은 하지 않음)
  await page.goto(`${baseUrl}/signup`, { waitUntil: 'networkidle' });
  await page.fill('#name', '홍길동');
  await page.fill('#email', 'newbie@bsg-demo.local');
  await page.fill('#phoneNumber', '01012345678');
  await page.fill('#password', 'sample1234');
  await page.fill('#confirmPassword', 'sample1234');
  // 실제 체크박스는 CSS 로 숨겨지고 꾸며진 span 이 클릭을 가로채므로 force 로 체크한다.
  await page.check('#termsAccepted', { force: true });
  await shoot(page, 'signup', '회원가입 화면');

  // 3) 로그인 (시드로 미리 심은 ADMIN 계정 — 권한 관리 화면 접근용)
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await page.fill('#email', adminEmail);
  await page.fill('#password', password);
  await Promise.all([
    page.waitForLoadState('networkidle'),
    page.click('button[type="submit"]'),
  ]);

  // 4) 회원 목록 화면
  await page.goto(`${baseUrl}/members`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800); // 그리드 렌더 대기
  await shoot(page, 'members', '회원 목록 화면');

  // 5) 문의 목록 화면
  await page.goto(`${baseUrl}/inquiries`, { waitUntil: 'networkidle' });
  await shoot(page, 'inquiries', '문의 목록 화면');

  // 6) 문의 작성 화면
  await page.goto(`${baseUrl}/inquiries/new`, { waitUntil: 'networkidle' });
  await shoot(page, 'inquiry-new', '문의 작성 화면');

  // 7) 권한 관리 화면 (ADMIN 전용)
  await page.goto(`${baseUrl}/admin/access`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800); // 그리드 렌더 대기
  await shoot(page, 'admin-access', '권한 관리 화면');

  await browser.close();
};

try {
  await run();
} catch (e) {
  console.log(`[ERROR] 캡처 중단: ${e}`);
} finally {
  const ok = shots.filter(s => s.ok).length;
  const fail = shots.length - ok;
  console.log('RESULT_JSON ' + JSON.stringify({ total: shots.length, ok, fail, shots }));
  process.exit(fail > 0 ? 1 : 0);
}
