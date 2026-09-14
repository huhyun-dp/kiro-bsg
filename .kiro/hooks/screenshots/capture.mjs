// 작업(변경)된 화면만 건별 PNG 로 캡처하는 Playwright 스크립트.
// 오케스트레이터(capture-screens.ps1)가 앱을 띄운 뒤, 캡처할 "화면 키" 목록을 넘겨 호출한다.
//
// 사용법:
//   node capture.mjs <baseUrl> <outDir> <adminEmail> <password> <screenKeys>
//     screenKeys: 쉼표로 구분한 화면 키. 예) "login,signup"  (비어 있으면 아무것도 캡처하지 않음)
//
// 표준출력 마지막 줄에 결과 JSON 을 찍는다: {"total":N,"ok":N,"fail":N,"shots":[...]}

import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';

const [baseUrl, outDir, adminEmail, password, screenKeysArg = ''] = process.argv.slice(2);
if (!baseUrl || !outDir || !adminEmail || !password) {
  console.error('usage: node capture.mjs <baseUrl> <outDir> <adminEmail> <password> <screenKeys>');
  process.exit(2);
}

// ── 화면 카탈로그 ──────────────────────────────────────────────
// key: 화면 식별자, url: 이동 경로, description: 사람이 읽는 이름
// needsAdmin: 로그인(ADMIN) 상태가 필요한지, prepare: 캡처 전 입력/대기 등 추가 동작
const CATALOG = {
  login: {
    url: '/login',
    description: '로그인 화면',
    needsAdmin: false,
  },
  signup: {
    url: '/signup',
    description: '회원가입 화면',
    needsAdmin: false,
    prepare: async (page) => {
      await page.fill('#name', '홍길동');
      await page.fill('#email', 'newbie@bsg-demo.local');
      await page.fill('#phoneNumber', '01012345678');
      await page.fill('#password', 'sample1234');
      await page.fill('#confirmPassword', 'sample1234');
      // 실제 체크박스는 CSS 로 숨겨지고 꾸며진 span 이 클릭을 가로채므로 force 로 체크한다.
      await page.check('#termsAccepted', { force: true });
    },
  },
  members: {
    url: '/members',
    description: '회원 목록 화면',
    needsAdmin: true,
    prepare: async (page) => { await page.waitForTimeout(800); }, // 그리드 렌더 대기
  },
  inquiries: {
    url: '/inquiries',
    description: '문의 목록 화면',
    needsAdmin: true,
  },
  'inquiry-new': {
    url: '/inquiries/new',
    description: '문의 작성 화면',
    needsAdmin: true,
  },
  'admin-access': {
    url: '/admin/access',
    description: '권한 관리 화면',
    needsAdmin: true,
    prepare: async (page) => { await page.waitForTimeout(800); }, // 그리드 렌더 대기
  },
};

const screenKeys = screenKeysArg.split(',').map(s => s.trim()).filter(Boolean);
const unknown = screenKeys.filter(k => !CATALOG[k]);
if (unknown.length) {
  console.log(`[WARN] 알 수 없는 화면 키 무시: ${unknown.join(', ')}`);
}
const targets = screenKeys.filter(k => CATALOG[k]);

fs.mkdirSync(outDir, { recursive: true });

const shots = [];
let seq = 0;

async function shoot(page, key, description) {
  seq += 1;
  const file = path.join(outDir, `${String(seq).padStart(2, '0')}-${key}.png`);
  try {
    await page.screenshot({ path: file, fullPage: true });
    shots.push({ name: key, description, file, ok: true });
    console.log(`[OK]   ${path.basename(file)} — ${description}`);
  } catch (e) {
    shots.push({ name: key, description, file, ok: false, error: String(e) });
    console.log(`[FAIL] ${path.basename(file)} — ${description}: ${e}`);
  }
}

async function loginAsAdmin(page) {
  await page.goto(`${baseUrl}/login`, { waitUntil: 'networkidle' });
  await page.fill('#email', adminEmail);
  await page.fill('#password', password);
  await Promise.all([
    page.waitForLoadState('networkidle'),
    page.click('button[type="submit"]'),
  ]);
}

const run = async () => {
  if (targets.length === 0) {
    console.log('[INFO] 캡처할 화면이 없습니다(변경된 화면 없음).');
    return;
  }

  const browser = await chromium.launch();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // login/signup 같은 미인증 전용 화면은 로그인하면 리다이렉트되므로,
  // 반드시 "미인증 화면 먼저 → 로그인 → 인증 화면" 순서로 캡처한다.
  const publicTargets = targets.filter(k => !CATALOG[k].needsAdmin);
  const adminTargets  = targets.filter(k => CATALOG[k].needsAdmin);
  const orderedTargets = [...publicTargets, ...adminTargets];

  let loggedIn = false;
  for (const key of orderedTargets) {
    const def = CATALOG[key];
    // 인증 화면 캡처 직전에 한 번만 로그인
    if (def.needsAdmin && !loggedIn) {
      await loginAsAdmin(page);
      loggedIn = true;
    }
    await page.goto(`${baseUrl}${def.url}`, { waitUntil: 'networkidle' });
    if (def.prepare) {
      await def.prepare(page);
    }
    await shoot(page, key, def.description);
  }

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
