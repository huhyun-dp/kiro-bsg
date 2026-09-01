---
inclusion: manual
---

# Development Process (개발 프로세스)

이 문서는 기존 서비스의 기능 수정 개발을 **Developer-Assisted Mode**와
**Autonomous Mode** 두 가지로 나누어 안전하게 진행하기 위한 공통 프로세스 규칙이다.

- `inclusion: manual` 이므로 모든 세션에 자동 주입되지 않는다.
- `developer` / `autonomous` Custom Agent 의 `resources` 로 참조되어, 해당 Agent 사용 시에만 규칙이 적용된다.
- 기존 always steering(`product.md`, `structure.md`, `tech.md`)과 충돌하지 않으며, 그 내용을 대체하지 않는다.

이 문서는 프로세스 규칙만 정의한다. 기존 소스코드의 기능 변경/리팩토링을 지시하지 않는다.

---

## 1. 공통 개발 Lifecycle

두 모드 모두 다음 lifecycle을 기준으로 한다.

```
Understand → Analyze → Plan → Approve → Implement → Validate → Review → Commit → Deploy
```

| 단계 | 책임 |
|------|------|
| **Understand** | 요구사항의 의도를 파악한다. 불명확하면 질문한다(추측 금지). |
| **Analyze** | 관련 소스코드를 실제로 읽고, 영향 범위(파일/계층/테스트)를 식별한다. 코드를 추측하지 않는다. |
| **Plan** | 변경 범위, 수정 대상 파일, 구현 방법, 테스트 계획, regression 영향, 위험 요소를 제시한다. |
| **Approve** | 계획에 대한 승인 게이트. 모드에 따라 개발자 승인 또는 세션 시작 시 부여된 범위 승인으로 갈린다. |
| **Implement** | 승인된 범위 안에서만 코드를 수정한다. 무관한 리팩토링/삭제 금지. |
| **Validate** | 빌드(`./mvnw clean package`)와 관련 테스트(`./mvnw test`)를 실제로 실행해 검증한다. |
| **Review** | 변경 요약, 실행한 검증 결과(성공/실패), 남은 위험을 보고한다. |
| **Commit** | 검증 통과 후 커밋. Conventional Commits 스타일 유지. |
| **Deploy** | 이번 단계에서는 실제 배포를 구성하지 않는다. 향후 확장 지점으로만 둔다. |

> 검증 원칙: 실제로 실행하지 않은 테스트를 성공했다고 보고하지 않는다.
> 검증할 수 없었던 항목은 "검증하지 못함"으로 명시한다.

---

## 2. Developer-Assisted Mode

**목적:** 개발자와 Kiro가 협업하며 안전하게 기존 서비스를 수정한다.
**권장 조합:** `developer` Agent + **Supervised** autonomy.

### Lifecycle

```
Understand → Analyze → Plan → Developer Approve → Implement → Validate
→ Developer Review → Developer Approve → Commit → Deploy
```

### 규칙

- 항상 기존 코드 분석을 먼저 수행한다.
- 요구사항을 확인하고, 관련 파일과 영향 범위를 분석한다.
- 구현 계획을 먼저 제시한다.
- **개발자의 명시적 Approve 전에는 소스코드를 수정하지 않는다.**
- 승인된 범위만 구현한다.
- 관련 테스트를 수행하고 regression 영향을 확인한다.
- 결과를 보고한다.
- **최종 개발자 검증 전에는 Commit / Push / Deploy 를 수행하지 않는다.**
- 요구사항 범위를 벗어난 변경이 필요하면 작업을 멈추고 다시 승인을 요청한다.
- 관련 없는 리팩토링을 하지 않는다.
- 기존 기능을 임의로 삭제하거나 변경하지 않는다.
- 코드나 동작을 추측하지 않고 실제 소스코드를 확인한다.

---

## 3. Autonomous Mode

**목적:** 개발자 개입을 최소화하고 승인된 개발 작업을 자동으로 수행한다.
**권장 조합:** `autonomous` Agent + **Autopilot** autonomy.

### Lifecycle

```
Understand → Analyze → Plan → Implement → Validate → Regression Test → Review → Commit
```

향후 확장 시:

```
→ Push → CI/CD → Automated Validation → Deploy Approval 또는 자동 Deploy
```

### 규칙

- 요구사항 분석 → 기존 코드 분석 → 영향 범위 분석 → 구현 계획 수립을 수행한다.
- 승인된 범위 안에서 코드를 자동으로 구현한다.
- 테스트를 실행한다.
- 검증 실패 시 원인을 분석하고 **가능한 범위에서** 자동 수정한다.
- Regression 검증을 수행한다.
- 최종 결과를 보고한다.
- 검증을 통과한 경우 Commit 할 수 있다.
- **Git Push 와 Deploy 는 현재 단계에서 개발자 확인이 필요하다.**
- 아래 "금지된 작업"은 기본적으로 자동 수행하지 않는다.

---

## 4. 승인 기준 (Approval)

- **Developer-Assisted:** 코드 수정 전, 그리고 Commit 전에 개발자의 명시적 승인이 필요하다.
- **Autonomous:** 세션 시작 시 부여된 작업 범위가 승인 범위다. 이 범위를 벗어나는 변경(다른 기능, 아키텍처 변경, 요청되지 않은 파일 수정)은 자동 수행하지 않고 승인을 요청한다.
- 두 모드 공통: Push / Deploy / destructive / 보안·인프라 변경은 별도 확인 없이는 진행하지 않는다.

## 5. 구현 범위 기준 (Scope)

- 요청된 문제만 해결한다. 요청 범위 밖의 개선/추상화/방어 코드를 임의로 추가하지 않는다.
- 버그 수정 시 주변 코드를 청소하지 않는다. 단순 기능에 불필요한 설정 가능성을 추가하지 않는다.
- 기존 기능을 삭제/변경하지 않는다.
- 프로젝트 기존 컨벤션(헥사고날 계층, 수동 빈 등록, MyBatis XML 매퍼 등)을 따른다.

## 6. 테스트 기준 (Test)

- 기능 추가/버그 수정 시 관련 테스트를 실행한다: `./mvnw test`
- 전체 빌드 검증: `./mvnw clean package`
- 새 동작에 대한 테스트가 없다면 프로젝트의 기존 테스트 스타일(JUnit + Spring Boot Test, H2 격리 프로파일)을 따라 추가를 고려한다.
- 실행하지 않은 테스트를 성공으로 보고하지 않는다.

## 7. Regression 검증 기준

- 변경으로 영향받을 수 있는 계층(controller / service / form / query)의 기존 테스트를 실행해 회귀 여부를 확인한다.
- 최소한 관련 테스트, 가능하면 전체 테스트 스위트를 실행한다.
- 회귀가 발견되면 원인을 분석하고, 승인된 범위 안에서 수정하거나 개발자에게 보고한다.

## 8. Commit 기준

- 빌드와 관련 테스트가 통과한 후에만 커밋한다.
- Conventional Commits 스타일과 기존 저장소의 커밋 메시지 언어(한국어)를 유지한다.
- 특정 파일을 지정해 스테이징한다(`git add <파일>`). 무관한 변경을 함께 커밋하지 않는다.
- 비밀정보가 담길 수 있는 파일(`.env`, credentials 등)은 커밋하지 않는다.
- `git commit --amend` 대신 새 커밋을 선호한다. hook(`--no-verify`)을 임의로 건너뛰지 않는다.

## 9. Deploy 기준

- 이번 단계에서는 실제 Production Deploy를 구성하지 않는다.
- 배포 관련 작업(예: `docker compose up`, 배포 스크립트)은 개발자 확인이 필요하다.
- 향후 CI/CD 연계 확장 지점은 아래 11절 참고.

## 10. 금지된 작업 (Destructive / High-risk)

다음은 기본적으로 자동 수행하지 않으며, permission 상 deny 또는 ask 로 통제한다.

- Production 배포
- 보안 설정 변경
- Credential 변경
- 인프라 변경
- 대규모 삭제 / 재귀 삭제 (`rm -rf`, `Remove-Item -Recurse -Force` 등)
- 데이터 삭제 (`DROP`, `TRUNCATE`, `DELETE FROM` 등)
- `git reset --hard`, `git clean`, `git push --force`
- `sudo`, 시스템 레벨 변경
- 기타 되돌리기 어려운 destructive operation

향후 별도 승인을 통해 자동화 범위를 확장할 수 있다(예: push 자동화). 이 경우 permission 규칙을 명시적으로 완화한다.

## 11. 요구사항이 불명확할 때

- 추측해서 구현하지 않는다.
- 가장 유용할 것으로 판단되는 방향을 제시하고 필요한 정보를 질문한다.
- 두 가지 이상 해석이 가능하면 옵션을 제시하고 개발자의 선택을 기다린다.

## 12. 승인된 범위를 벗어나는 변경이 필요할 때

- 즉시 작업을 중단한다.
- 왜 범위를 벗어나는지, 어떤 추가 변경이 필요한지, 어떤 트레이드오프가 있는지 설명한다.
- 개발자의 재승인을 받은 후에만 진행한다.
- 요청된 기능/요구사항을 임의로 축소하거나 제거하지 않는다(최후의 수단).

---

## 13. Autopilot / Supervised 와의 관계

- Autopilot / Supervised 는 Kiro의 **전역 autonomy 설정**(`Settings → Agent → Agent Autonomy`)이며 Custom Agent 설정으로 강제 전환할 수 없다.
- 모드 구현은 **Custom Agent + Steering(이 문서) + Permission** 조합으로 하고, autonomy 설정은 권장 사항으로 둔다.
  - Developer-Assisted Mode: `developer` Agent + **Supervised** 권장
  - Autonomous Mode: `autonomous` Agent + **Autopilot** 권장
- Agent 별 permission 이 fine-grained 안전장치로 동작하며, deny-overrides(deny > ask > allow) 규칙을 따른다.
- Kiro 자체 불변식(settings 쓰기 deny, `.git/**`·`.kiro/agents/**`·`.kiro/hooks/**` 쓰기 ask)은 우회하지 않으며 그대로 존중한다.

---

## 14. 향후 확장 지점 (Hooks / CI/CD / 무인 배포)

이번 단계에서는 구성하지 않는다. 아래는 확장 계획만 문서화한 것이다.

### 14-1. Hooks (미생성)

향후 필요 시 `.kiro/hooks/` 에 추가할 수 있다. 이 프로젝트에서 실질적으로 유용한 후보는 다음 하나다.

- **PostTaskExec 기반 Maven 테스트 자동화**: Spec task 완료 후 `./mvnw test` 를 실행해 regression 을 자동 검증.
  - 예시 스키마(참고용, 이번엔 생성하지 않음):
    ```json
    {
      "version": "v1",
      "hooks": [{
        "name": "Run Maven Tests After Task",
        "trigger": "PostTaskExec",
        "action": { "type": "command", "command": "./mvnw test" }
      }]
    }
    ```
- lint / type-check Hook 은 현재 Java/Maven 구성에 대응 도구가 없어 추가하지 않는다(불필요한 Hook 지양).

### 14-2. Git / CI/CD 확장

**Developer-Assisted:**

```
Kiro → Developer Review → Commit → Developer Push → CI/CD → Developer Deploy Approval
```

**Autonomous:**

```
Kiro → Implement → Test → Regression → Commit → Push → CI/CD
→ Automated Validation → Deploy Approval 또는 자동 Deploy
```

확장 방법:
- 현재 `git push` 와 deploy 는 permission 상 `ask` 로 걸려 있다. CI/CD 연계 시 이 지점만 명시적으로 완화하면 된다.
- 실제 배포 파이프라인(GitHub Actions 등)은 별도 승인 후 구성한다.
- 무인 배포는 자동 검증(빌드/테스트/regression) 통과를 전제로 하며, Production 배포는 마지막까지 승인 게이트를 유지하는 것을 기본으로 한다.

### 14-3. 현재 제한사항

- Push / Deploy / CI/CD 는 실제로 구성되어 있지 않다.
- Hook 은 생성되어 있지 않다(확장 지점만 문서화).
- Autonomous Mode 도 push/deploy/destructive/보안·인프라 변경은 자동 수행하지 않는다.
