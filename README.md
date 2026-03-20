# slog-2026-03

이 리포지토리는 Next.js 기반 클라이언트 프론트엔드, Kotlin + Spring Boot API 백엔드, 그리고 이를 호스팅하기 위한 Terraform + AWS EC2 부트스트랩 인프라로 구성된 풀스택 서비스 프로젝트입니다.
본 루트 문서는 하위 도메인별 문서들의 빠른 접근을 돕는 통합 허브(Hub)입니다.

---

## 🏗️ 전체 아키텍처 개요

```mermaid
flowchart TD
    Client[Browser / Next.js Client] -->|HTTP / WebSocket| NPMplus[Nginx Proxy Manager]
    
    subgraph AWS_EC2 [AWS EC2 t3.micro]
        NPMplus -->|API Requests| SpringBoot[Spring Boot Backend]
        NPMplus -->|SSE / WebSockets| SpringBoot
        SpringBoot --> Redis[(Redis)]
        SpringBoot --> PostgreSQL[(PostgreSQL)]
    end

    subgraph GitHub_Actions [GitHub Actions]
        Push[Push to main] --> Build[Build & Push to GHCR]
        Build --> Deploy[SSM Blue/Green Deploy]
        Deploy --> NPMplus
    end
```

---

## 📚 프로젝트 구성 및 메뉴얼 허브

이 프로젝트는 역할을 명확히 분리하기 위해 4개의 핵심 디렉토리로 구성되어 있습니다. 각 디렉토리에는 심층적인 기술 및 설계 방법이 담긴 개별 문서가 존재합니다.

| 서비스 영역 | 경로 | 역할 및 기술 스택 | 문서 링크 |
|------------|----------|-------------------|-----------|
| **Backend** | `back/` | Kotlin, Spring Boot, JPA, QueryDSL, OAuth2 | [📖 백엔드 63강 설계 리뷰 보기](back/README.md) |
| **Frontend** | `front/` | Next.js (App Router), TypeScript, Tailwind | [📖 프론트엔드 연동 가이드 보기](front/README.md) |
| **Infra** | `infra/` | Terraform, AWS EC2, VPC, Security Group, Docker bootstrap | [📖 인프라 프로비저닝 가이드 보기](infra/README.md) |
| **CI/CD** | `.github/workflows/` | GitHub Actions, GHCR, Blue/Green Deploy | [📖 자동화 배포 파이프라인 가이드 보기](.github/workflows/README.md) |

---

## 🚀 로컬 환경 빠른 시작

각 파트별 상세 설정은 상단의 문서 링크들을 참고하시고, 로컬에서 가장 빠르게 띄워보는 방법은 아래와 같습니다.

### 1) 백엔드 기동
```bash
cd back
# PostgreSQL / Redis가 준비되어 있어야 함
./gradlew bootRun
```

### 2) 프론트엔드 기동
```bash
cd front
pnpm install
pnpm dev
# http://localhost:3000 접속
```

### 3) 인프라 프로비저닝 (AWS 계정 필요)
```bash
cd infra
cp secrets.tf.default secrets.tf # 민감 정보 입력
terraform init
terraform plan
terraform apply
```
