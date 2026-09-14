# Hook 실행 로그

이 폴더에는 hook 이 실제로 동작했는지 확인할 수 있는 로그가 쌓인다.

## run-maven-tests.log

`작업 완료 후 기존 기능 동작 확인` hook 이 실행될 때마다 한 줄씩 기록된다.

형식:

```
[2026-01-01 12:00:00] START  ./mvnw test
[2026-01-01 12:01:30] RESULT exitCode=0 (PASS)
```

- `START` 줄: hook 이 걸려서 테스트 실행을 시작한 시각
- `RESULT` 줄: 테스트가 끝난 시각과 결과(`exitCode=0` 이면 PASS, 그 외 FAIL)

이 파일에 최신 줄이 추가되어 있으면 hook 이 설정대로 동작한 것이다.
