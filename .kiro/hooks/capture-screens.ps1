# 작업 완료 후 주요 화면을 건별 PNG 로 캡처하는 hook 의 실제 동작 스크립트.
# 1) 앱을 screenshot 프로파일(H2 인메모리)로 기동
# 2) 준비될 때까지 대기
# 3) Playwright 로 화면 캡처(회원가입->로그인->각 화면)
# 4) 앱 종료
# 5) 공통 로그 템플릿으로 결과 기록
#
# 캡처 결과: .kiro/hooks/screenshots/out/<타임스탬프>/*.png
# 로그:      .kiro/hooks/logs/capture-screens.log

$ErrorActionPreference = 'Continue'
$ProgressPreference    = 'SilentlyContinue'  # Invoke-WebRequest 진행률 표시로 콘솔이 지저분해지는 것 방지

. "$PSScriptRoot\_hook-log.ps1"

$repoRoot   = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$shotRoot   = Join-Path $PSScriptRoot 'screenshots'
$stamp      = Get-Date -Format 'yyyyMMdd-HHmmss'
$outDir     = Join-Path $shotRoot ("out\" + $stamp)
$captureJs  = Join-Path $shotRoot 'capture.mjs'

# 템플릿 파일 경로 → 화면 키 매핑. 변경된 템플릿에 해당하는 화면만 캡처한다.
$templateScreenMap = @{
  'src/main/resources/templates/auth/login.html'     = 'login'
  'src/main/resources/templates/auth/signup.html'    = 'signup'
  'src/main/resources/templates/members.html'        = 'members'
  'src/main/resources/templates/inquiry/list.html'   = 'inquiries'
  'src/main/resources/templates/inquiry/form.html'   = 'inquiry-new'
  'src/main/resources/templates/inquiry/detail.html' = 'inquiries'   # 상세는 목록 경유로 대체 캡처
  'src/main/resources/templates/admin/access.html'   = 'admin-access'
}
# 공통 레이아웃이 바뀌면 모든 화면에 영향 → 대표 화면 전체를 대상으로 한다.
$layoutAffectedScreens = @('members', 'inquiries', 'inquiry-new', 'admin-access')

# 작업(변경)된 템플릿을 git 으로 감지 (스테이징 + 미스테이징 + 미추적)
function Get-ChangedScreenKeys {
  Push-Location $repoRoot
  try {
    $changed = @()
    $changed += (git diff --name-only 2>$null)
    $changed += (git diff --name-only --cached 2>$null)
    $changed += (git ls-files --others --exclude-standard 2>$null)
    $changed = $changed | Where-Object { $_ } | ForEach-Object { $_.Trim() } | Sort-Object -Unique
  } finally {
    Pop-Location
  }

  $keys = New-Object System.Collections.Generic.List[string]
  foreach ($f in $changed) {
    $norm = $f.Replace('\', '/')
    if ($norm -eq 'src/main/resources/templates/fragments/layout.html') {
      $layoutAffectedScreens | ForEach-Object { $keys.Add($_) }
    } elseif ($templateScreenMap.ContainsKey($norm)) {
      $keys.Add($templateScreenMap[$norm])
    }
  }
  return ($keys | Sort-Object -Unique)
}

$port       = 18080
$baseUrl    = "http://localhost:$port"
$adminEmail = 'capture-admin@bsg-demo.local'
$password   = 'capture1234'

$startTime  = Get-Date
$appProc    = $null
$captureOut = ''
$total = $ok = $fail = '-'
$phase = '초기화'
$screenKeys = @()

try {
  # --- 0) 작업된 화면 판별 ---
  $phase = '변경 화면 판별'
  $screenKeys = @(Get-ChangedScreenKeys)
  if ($screenKeys.Count -eq 0) {
    # 변경된 화면이 없으면 앱을 띄우지 않고 종료(불필요한 캡처 방지)
    $phase = '변경 화면 없음'
    $total = 0; $ok = 0; $fail = 0
    Write-Output '[INFO] 변경된 화면이 없어 캡처를 건너뜁니다.'
    return
  }
  Write-Output ("[INFO] 캡처 대상 화면: {0}" -f ($screenKeys -join ', '))

  New-Item -ItemType Directory -Force -Path $outDir | Out-Null

  # --- 1) 앱 기동 ---
  $phase = '앱 기동'
  $jar = Get-ChildItem (Join-Path $repoRoot 'target\*.jar') -ErrorAction SilentlyContinue |
         Where-Object { $_.Name -notlike '*sources*' -and $_.Name -notlike '*javadoc*' } |
         Select-Object -First 1
  if (-not $jar) { throw '실행 가능한 jar 를 찾을 수 없습니다. 먼저 ./mvnw package 로 빌드하세요.' }

  # screenshot 프로파일은 H2 를 쓰지만 H2 는 test scope 라 jar 에 없다.
  # pom.xml 을 건드리지 않고, 로컬 m2 의 H2 jar 를 loader.path 로 외부 주입한다.
  $h2 = Get-ChildItem "$env:USERPROFILE\.m2\repository\com\h2database\h2" -Recurse -Filter 'h2-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*sources*' -and $_.Name -notlike '*javadoc*' } |
        Select-Object -First 1
  if (-not $h2) { throw '로컬 m2 에서 H2 드라이버 jar 를 찾을 수 없습니다.' }

  $env:SPRING_PROFILES_ACTIVE = 'screenshot'
  $env:SERVER_PORT            = "$port"
  $env:BOOTSTRAP_ADMIN_EMAIL  = $adminEmail
  $env:SEED_DEMO_MEMBERS      = 'true'

  $appLog = Join-Path $outDir 'app.log'
  $appProc = Start-Process -FilePath 'java' `
      -ArgumentList @("-Dloader.path=$($h2.FullName)", '-cp', $jar.FullName, 'org.springframework.boot.loader.PropertiesLauncher') `
      -RedirectStandardOutput $appLog `
      -RedirectStandardError (Join-Path $outDir 'app.err.log') `
      -PassThru -NoNewWindow

  # --- 2) 준비 대기 (로그인 페이지 200 응답) ---
  $phase = '앱 준비 대기'
  $ready = $false
  for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    if ($appProc.HasExited) { throw "앱이 기동 중 종료되었습니다(exit=$($appProc.ExitCode)). app.log 확인." }
    try {
      $resp = Invoke-WebRequest -Uri "$baseUrl/login" -UseBasicParsing -TimeoutSec 3
      if ($resp.StatusCode -eq 200) { $ready = $true; break }
    } catch { }
  }
  if (-not $ready) { throw '앱이 제한 시간 내에 준비되지 않았습니다.' }

  # --- 3) 캡처 (변경된 화면만) ---
  $phase = '화면 캡처'
  $screenArg = ($screenKeys -join ',')
  Push-Location $shotRoot
  $captureOut = & node $captureJs $baseUrl $outDir $adminEmail $password $screenArg 2>&1 | Out-String
  $captureExit = $LASTEXITCODE
  Pop-Location
  Write-Output $captureOut

  $jsonLine = ($captureOut -split "`r?`n" | Where-Object { $_ -match '^RESULT_JSON ' } | Select-Object -Last 1)
  if ($jsonLine) {
    $parsed = ($jsonLine -replace '^RESULT_JSON ', '') | ConvertFrom-Json
    $total = $parsed.total; $ok = $parsed.ok; $fail = $parsed.fail
  }

  $phase = '완료'
}
catch {
  $captureOut += "`n[ERROR] $($_.Exception.Message)"
  Write-Output "[ERROR] $($_.Exception.Message)"
}
finally {
  # --- 4) 앱 종료 ---
  if ($appProc -and -not $appProc.HasExited) {
    try { Stop-Process -Id $appProc.Id -Force -ErrorAction SilentlyContinue } catch { }
  }
  Remove-Item Env:SPRING_PROFILES_ACTIVE, Env:BOOTSTRAP_ADMIN_EMAIL -ErrorAction SilentlyContinue

  $endTime  = Get-Date
  $duration = [math]::Round(($endTime - $startTime).TotalSeconds, 1)
  $result   = if ($fail -eq 0 -and $total -ne '-') { 'PASS' } else { 'FAIL' }

  $targetLabel = if ($screenKeys.Count -gt 0) { ($screenKeys -join ', ') } else { '(변경된 화면 없음)' }

  $fields = [ordered]@{
    '실행 ID'   = [guid]::NewGuid().ToString('N').Substring(0, 8)
    'Hook 이름' = '작업 완료 후 화면 캡처(변경분만)'
    '트리거'    = 'postTaskExecution (Spec 작업 완료)'
    '_sep1'     = '---'
    '시작 시각' = $startTime.ToString('yyyy-MM-dd HH:mm:ss')
    '종료 시각' = $endTime.ToString('yyyy-MM-dd HH:mm:ss')
    '소요 시간' = "${duration}s"
    '단계'      = $phase
    '대상 화면' = $targetLabel
    '_sep2'     = '---'
    '캡처 총계' = "$total (성공 $ok / 실패 $fail)"
    '저장 위치' = $outDir
    '최종 결과' = $result
  }
  Write-HookLog -LogName 'capture-screens' -Title 'Hook 실행 기록 (capture-screens)' -Fields $fields
}
