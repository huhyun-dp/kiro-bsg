# 작업 완료 후 기존 기능 동작 확인 hook 의 실제 동작 스크립트.
# ./mvnw test 를 실행하고, 결과를 공통 로그 템플릿(Write-HookLog)으로 기록한다.
# 로그 위치: .kiro/hooks/logs/run-maven-tests.log

$ErrorActionPreference = 'Continue'

. "$PSScriptRoot\_hook-log.ps1"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')

$startTime = Get-Date
$command   = './mvnw test'

# --- 테스트 실행 (출력은 화면과 캡처 양쪽으로) ---
Push-Location $repoRoot
$output = & cmd /c "mvnw.cmd test" 2>&1 | Tee-Object -Variable captured
$exitCode = $LASTEXITCODE
Pop-Location

$endTime  = Get-Date
$duration = [math]::Round(($endTime - $startTime).TotalSeconds, 1)
$result   = if ($exitCode -eq 0) { 'PASS' } else { 'FAIL' }

# --- Maven surefire 요약 라인에서 테스트 통계 파싱 ---
# 예: "Tests run: 42, Failures: 0, Errors: 0, Skipped: 1"
$total = $fail = $err = $skip = '-'
$summaryLine = $captured |
  Select-String -Pattern 'Tests run: \d+, Failures: \d+, Errors: \d+, Skipped: \d+' |
  Select-Object -Last 1
if ($summaryLine) {
  if ($summaryLine.Line -match 'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)') {
    $total = $Matches[1]; $fail = $Matches[2]; $err = $Matches[3]; $skip = $Matches[4]
  }
}

# --- BUILD 결과 라인 ---
$buildLine = ($captured | Select-String -Pattern 'BUILD (SUCCESS|FAILURE)' | Select-Object -Last 1)
$build = if ($buildLine) { ($buildLine.Line -replace '.*(BUILD (SUCCESS|FAILURE)).*', '$1') } else { 'UNKNOWN' }

# --- 공통 템플릿으로 로그 기록 ---
$fields = [ordered]@{
  '실행 ID'   = [guid]::NewGuid().ToString('N').Substring(0, 8)
  'Hook 이름' = '작업 완료 후 기존 기능 동작 확인'
  '트리거'    = 'postTaskExecution (Spec 작업 완료)'
  '실행 명령' = $command
  '_sep1'     = '---'
  '시작 시각' = $startTime.ToString('yyyy-MM-dd HH:mm:ss')
  '종료 시각' = $endTime.ToString('yyyy-MM-dd HH:mm:ss')
  '소요 시간' = "${duration}s"
  '_sep2'     = '---'
  '테스트 총계' = "$total (실패 $fail / 오류 $err / 건너뜀 $skip)"
  '빌드 결과' = $build
  '종료 코드' = $exitCode
  '최종 결과' = $result
}

Write-HookLog -LogName 'run-maven-tests' -Title 'Hook 실행 기록 (run-maven-tests)' -Fields $fields

exit $exitCode
