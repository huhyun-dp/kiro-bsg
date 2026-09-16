# Kiro AWS MCP 연동 (EC2·RDS 배포 한정)

이 문서는 Kiro가 MCP를 통해 이 프로젝트의 배포 작업(EC2·RDS 및 관련 네트워킹·비용 알림)만 수행하도록 안전하게 연동하는 절차를 설명한다.

핵심 원칙은 두 가지다.

- **실제 권한 경계는 IAM으로 정한다.** MCP config의 `autoApprove`는 승인 편의 설정일 뿐 권한 경계가 아니다.
- **자격증명은 파일에 키로 넣지 않는다.** 로컬 AWS 프로파일(`AWS_PROFILE`)을 참조한다.

> Terraform으로 이미 인프라를 코드화해 두었다. 권장 구성은 "생성·삭제는 Terraform, MCP는 조회·검증·보조"다. MCP로 직접 리소스를 만들 수도 있으나, 되돌리기 어려운 작업은 Kiro가 실행 전 확인을 받는다.

## 1) 권한을 제한한 IAM 자격증명 만들기

1. IAM 정책을 생성한다. 정책 문서는 [`iam-mcp-deploy-policy.json`](iam-mcp-deploy-policy.json)를 사용한다.
   - EC2·RDS는 `ap-northeast-2` 리전으로 제한한다.
   - Budgets는 비용 알림 관리용으로 허용한다.
   - IAM 액션은 `bsg-session-auth-*` 이름의 역할·인스턴스 프로파일에만 허용한다(Terraform의 `enable_ssm=true`일 때만 필요).
   - `sts:GetCallerIdentity`, `pricing`, `ce`는 검증·비용 확인용 읽기 권한이다.

   콘솔: IAM → 정책 → 정책 생성 → JSON 탭에 파일 내용 붙여넣기 → 이름 예: `bsg-session-auth-deploy`.

2. 이 정책을 붙인 **전용 IAM 사용자**(프로그래밍 방식 액세스)를 만들거나, 기존 사용자에 연결한다.
   - 사용자 예: `bsg-deploy`
   - 액세스 키를 발급받는다.

> 최소권한 주의: 위 정책은 `ec2:*`, `rds:*`를 리전 조건으로 허용한다. 데모·개인 계정 기준의 실용적 범위다. 더 엄격히 하려면 필요한 액션만 열거하도록 좁히면 된다. 프로덕션 조직 계정에서는 반드시 축소·검토한다.

## 2) 로컬 AWS 프로파일 등록

발급받은 키를 `bsg-deploy` 프로파일로 저장한다. 키를 mcp.json 등 파일에 직접 쓰지 않는다.

```powershell
aws configure --profile bsg-deploy
# AWS Access Key ID     : <발급한 키>
# AWS Secret Access Key : <발급한 시크릿>
# Default region name   : ap-northeast-2
# Default output format  : json
```

확인:

```powershell
aws sts get-caller-identity --profile bsg-deploy
```

## 3) uv(uvx) 설치

AWS API MCP 서버는 `uvx`로 실행한다. `uv`가 필요하다.

```powershell
# Astral uv 설치 (미설치 시)
winget install --id=astral-sh.uv -e
# 또는 pip install uv
uvx --version
```

## 4) Kiro MCP 설정 추가

`~/.kiro/settings/mcp.json`(예: `C:\Users\<계정>\.kiro\settings\mcp.json`)에 아래를 추가한다. 이 파일은 사용자가 직접 편집해야 한다(Kiro 안전장치로 에이전트가 쓰지 못함).

```json
{
  "mcpServers": {
    "aws-api": {
      "command": "uvx",
      "args": ["awslabs.aws-api-mcp-server@latest"],
      "env": {
        "AWS_PROFILE": "bsg-deploy",
        "AWS_REGION": "ap-northeast-2",
        "FASTMCP_LOG_LEVEL": "ERROR"
      },
      "disabled": false,
      "autoApprove": []
    }
  }
}
```

- `AWS_PROFILE`은 2)에서 만든 제한된 프로파일을 가리킨다. 실제 권한은 이 프로파일의 IAM 정책이 정한다.
- `autoApprove: []`로 두면 모든 MCP 도구 호출을 실행 전에 확인받는다. 리소스 생성·삭제가 무단 실행되지 않는다.
- 설정 후 Kiro의 MCP 서버 목록에서 `aws-api`를 확인·재연결한다.

## 5) 사용 방식과 한계

- MCP가 붙어도 Kiro는 되돌리기 어려운 작업(리소스 생성·삭제·수정) 전에 확인을 받는다.
- 비용이 발생하는 작업은 당신의 계정·크레딧에 직접 반영된다.
- "EC2·RDS만" 제한은 MCP config가 아니라 IAM 정책으로 보장된다. 정책 밖 작업은 AccessDenied로 차단된다.
- 인프라 형상 관리는 Terraform을 기준으로 삼고, MCP는 상태 조회·가격 확인·문제 진단 등 보조로 쓰는 것을 권장한다.

## 참고: 무엇을 MCP로, 무엇을 Terraform으로

| 작업 | 권장 도구 |
|------|-----------|
| VPC·EC2·RDS·보안그룹·EBS 생성/삭제 | Terraform (`deploy/terraform`) |
| 배포 후 리소스 상태·엔드포인트 확인 | MCP 조회 또는 `terraform output` |
| 요금·비용 추정 확인 | MCP(`pricing`, `ce`) 또는 AWS Pricing Calculator |
| 예산 알림 점검 | Budgets 콘솔 또는 MCP |
