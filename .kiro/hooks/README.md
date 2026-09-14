# Hooks 작성 규칙

이 폴더의 자동화(hook)는 **`*.kiro.hook` 형식 하나로만** 정의한다.

## 필수 규칙

- hook 정의 파일은 반드시 `<이름>.kiro.hook` 형식(파일당 hook 1개)으로 만든다.
- 구형식 `{"version":"v1","hooks":[...]}` 배열형 `*.json` 으로 hook 을 정의하지 않는다.
  - 이 형식은 현재 Kiro 가 인식하는 표준이 아니며, 같은 hook 이 `.kiro.hook` 과 함께 있으면
    **Kiro 패널에 동일한 hook 이 중복으로 보인다.**
  - `.gitignore` 에서 `*.json` 을 무시하도록 해 두었다(단, `screenshots/package.json` 은 예외).
- 하나의 hook 은 하나의 `.kiro.hook` 파일로만 존재해야 한다. 같은 목적의 정의를 두 벌 만들지 않는다.

## 표준 형식 예시

```json
{
  "enabled": true,
  "name": "작업 완료 후 기존 기능 동작 확인",
  "description": "...",
  "version": "1",
  "when": { "type": "postTaskExecution" },
  "then": {
    "type": "runCommand",
    "command": "powershell -NoProfile -ExecutionPolicy Bypass -File .kiro/hooks/<script>.ps1"
  }
}
```

- `when.type`: `postTaskExecution`(작업 완료 자동) 또는 `userTriggered`(수동 실행) 등
- 무거운 작업(앱 기동 등)은 실행 스크립트(`.ps1`)로 분리하고 hook 에서는 그 스크립트만 호출한다.

## 현재 등록된 hook

| 파일 | 트리거 | 하는 일 |
|------|--------|---------|
| `run-maven-tests-after-task.kiro.hook` | 작업 완료 | 기존 기능 동작 확인(`./mvnw test`) |
| `capture-changed-screens-after-task.kiro.hook` | 작업 완료 | 변경된 화면만 캡처 |
| `check-service-health.kiro.hook` | 수동 | 비개발자용 상태 점검 |

## 실행 스크립트

- `run-maven-tests.ps1` / `capture-screens.ps1` / `check-service-health.ps1`: 각 hook 의 실제 동작
- `_hook-log.ps1`: 공통 로그 템플릿(`Write-HookLog`). 여러 스크립트가 dot-source 해 재사용
