# 여러 hook 스크립트가 공유하는 공통 로그 템플릿.
# 각 hook 스크립트에서 dot-sourcing 으로 불러 Write-HookLog 를 사용한다.
#   . "$PSScriptRoot\_hook-log.ps1"
#
# 로그는 .kiro/hooks/logs/<LogName>.log 에 템플릿 형태로 append 된다.

function Write-HookLog {
    param(
        # 로그 파일 이름(확장자 제외). 예: 'run-maven-tests' -> logs/run-maven-tests.log
        [Parameter(Mandatory = $true)] [string]   $LogName,
        # 카드 상단 제목. 예: 'Hook 실행 기록 (run-maven-tests)'
        [Parameter(Mandatory = $true)] [string]   $Title,
        # 본문 항목. [ordered]@{ '라벨' = '값'; ... } 형태. 값이 '---' 이면 구분선으로 출력.
        [Parameter(Mandatory = $true)] [System.Collections.IDictionary] $Fields
    )

    $logDir  = Join-Path $PSScriptRoot 'logs'
    $logFile = Join-Path $logDir ("{0}.log" -f $LogName)
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null

    $border = '=' * 64
    $rule   = '-' * 64

    # 라벨 폭을 가장 긴 라벨에 맞춰 정렬(구분선 표시용 '---' 제외)
    $labels = $Fields.Keys | Where-Object { $Fields[$_] -ne '---' }
    $pad    = 0
    foreach ($l in $labels) { if ($l.Length -gt $pad) { $pad = $l.Length } }

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add($border)
    $lines.Add(" $Title")
    $lines.Add($rule)
    foreach ($key in $Fields.Keys) {
        $value = $Fields[$key]
        if ($value -eq '---') {
            $lines.Add($rule)
        } else {
            $label = $key.PadRight($pad)
            $lines.Add(" $label : $value")
        }
    }
    $lines.Add($border)
    $lines.Add('')

    Add-Content -Path $logFile -Value ($lines -join [Environment]::NewLine)
}
