# "지금 기능이 정상인지 확인하기" hook 의 실제 동작 스크립트.
#
# 대상 사용자: 비개발자(기획·현업·SM 담당자 등).
# 사용 방법 : Kiro 의 Agent Hook 목록에서 이 hook 을 버튼처럼 눌러 수동 실행한다.
#
# 동작:
#   1) 기존 기능이 그대로 동작하는지 자동으로 확인한다(내부적으로 ./mvnw test 실행).
#   2) 그 결과를 개발 용어 없이 쉬운 말로 로그에 남긴다.
# 로그 위치: .kiro/hooks/logs/check-service-health.log

$ErrorActionPreference = 'Continue'

. "$PSScriptRoot\_hook-log.ps1"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')

$startTime = Get-Date

# --- 기존 기능 동작 확인 실행 (출력은 화면과 캡처 양쪽으로) ---
Push-Location $repoRoot
$output = & cmd /c "mvnw.cmd test" 2>&1 | Tee-Object -Variable captured
$exitCode = $LASTEXITCODE
Pop-Location

$endTime  = Get-Date
$duration = [math]::Round(($endTime - $startTime).TotalSeconds, 1)

# --- 결과 판정 ---
$isOk = ($exitCode -eq 0)

# --- 확인한 항목 개수(테스트 총계) 파싱 → 사람이 이해할 수 있는 문장으로 ---
# 예: "Tests run: 64, Failures: 0, Errors: 0, Skipped: 0"
$total = $fail = $err = $null
$summaryLine = $captured |
  Select-String -Pattern 'Tests run: \d+, Failures: \d+, Errors: \d+, Skipped: \d+' |
  Select-Object -Last 1
if ($summaryLine -and $summaryLine.Line -match 'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)') {
  $total = [int]$Matches[1]; $fail = [int]$Matches[2]; $err = [int]$Matches[3]
}

# --- 비개발자용 문장 구성 ---
if ($isOk) {
  $headline   = '정상입니다'
  $meaning    = '기존 화면과 기능이 예전 그대로 잘 동작합니다.'
} else {
  $headline   = '확인이 필요합니다'
  $meaning    = '일부 기능이 예전과 다르게 동작할 수 있습니다. 개발자에게 이 기록을 전달해 주세요.'
}

if ($null -ne $total) {
  $brokenCount = ($fail + $err)
  $checkedValue = "총 ${total}개"
  $problemValue = if ($brokenCount -eq 0) { '없음' } else { "${brokenCount}개" }
} else {
  $checkedValue = '(개수를 확인하지 못함)'
  $problemValue = '(확인하지 못함)'
}

# --- 공통 템플릿으로 로그 기록 ---
$fields = [ordered]@{
  '확인한 사람' = '비개발자(수동 실행)'
  '무엇을 했나요' = '지금 기능이 정상인지 확인하기'
  '_sep1'     = '---'
  '확인 시작' = $startTime.ToString('yyyy-MM-dd HH:mm:ss')
  '확인 종료' = $endTime.ToString('yyyy-MM-dd HH:mm:ss')
  '걸린 시간' = "${duration}초"
  '_sep2'     = '---'
  '결과'      = $headline
  '뜻'        = $meaning
  '점검한 기능 항목' = $checkedValue
  '문제가 발견된 항목' = $problemValue
}

Write-HookLog -LogName 'check-service-health' -Title '기능 정상 여부 확인 기록 (비개발자용)' -Fields $fields

# 화면에도 사람이 읽을 요약을 한 줄로 안내
Write-Host ''
Write-Host "[확인 결과] $headline - $meaning"

exit $exitCode
