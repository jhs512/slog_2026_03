# GitHub Actions 워크플로우

이 폴더는 서비스 배포 파이프라인을 구성합니다.  
실행 파일은 `.github/workflows/deploy.yml` 하나이며, `main` 브랜치에 `back` 관련 변경사항이 올라오면 **Blue/Green 무중단 배포**가 실행됩니다.

---

## 🔄 배포 파이프라인 개요

`deploy.yml`은 4개의 Job으로 구성됩니다.

1. **Tag Calculation (`calculateTag`)**
   - `mathieudutour/github-tag-action`을 `dry_run: true`로 호출해 다음 태그 값을 계산만 합니다.

2. **Docker Build & Push (`buildImageAndPush`)**
   - `back/`를 컨텍스트로 하여 Docker 이미지를 빌드합니다.
   - GHCR(`ghcr.io`)에 `latest`와 계산된 버전 태그를 같이 푸시합니다.
   - `cache-from`, `cache-to`를 써서 빌드 캐시를 활용합니다.

3. **Blue/Green 무중단 배포 (`deploy`)**
   - AWS 자격증명으로 SSM 실행 권한을 구성한 뒤, 타겟 EC2에 배포 스크립트를 보내 실행합니다.
   - NPMplus API로 대상 Proxy Host를 조회/생성하고, Green 슬롯 컨테이너를 기동합니다.
   - Green이 `http://{컨테이너 IP}:8080/actuator/health` 200 응답을 보일 때까지 대기한 뒤, Proxy Host의 upstream을 Green으로 전환합니다.
   - 전환 성공 시 기존 Blue 컨테이너는 종료/정리하고, 불필요한 로컬 이미지를 정리합니다.

4. **Tag & Release (`makeTagAndRelease`)**
   - 배포가 성공한 뒤 `github-tag-action`으로 실제 태그 생성 후 GitHub 릴리스를 발행합니다.

---

## 🔐 필요 GitHub Secrets

이 배포를 정상적으로 작동시키기 위해서는 GitHub 리포지토리의 `Settings > Secrets and variables > Actions` 메뉴에서 아래의 비밀키들이 설정되어야 합니다.

- **`AWS_REGION`**: SSM을 구동할 타겟 인프라의 AWS 리전 (예: `ap-northeast-2`)
- **`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`**: 대상 EC2를 관리하고 SSM 명령을 하달할 수 있는 권한을 가진 AWS IAM 사용자의 자격 증명 (인프라 생성 시 할당됨)
- **`DOT_ENV`**: 운영 `.env` 원본 텍스트(인코딩 전)  
  - 배포 스크립트는 실행 단계에서 Base64로 변환 후 `/tmp/.env`로 저장해 컨테이너에 주입합니다.

참고: 스크립트는 EC2의 `/etc/environment`에서 `PASSWORD_1`, `APP_1_DOMAIN`를 읽어 NPMplus 로그인 및 도메인 탐색에 사용합니다. 해당 값이 없다면 배포가 실패할 수 있습니다.

---

## ⚙️ 상세: Blue / Green 메커니즘

`deploy` 잡에서 동작하는 블루그린 배포 스크립트(`cat << 'SCRIPT_EOF'`)는 다음 로직을 거칩니다.

- **포트 충돌 회피**: 컨테이너 로컬 포트(`8080`)를 EC2 호스트 포트로 직접 노출하지 않고 도커 네트워크(`common`)에서 컨테이너명 기반으로 통신합니다.
- **NPMplus API 제어**: `http://127.0.0.1:81`의 NPMplus Admin API(`/api/tokens`, `/api/nginx/proxy-hosts`)로 로그인 후 프록시를 조회/갱신합니다.
- **Health Check 전략**: `Green` 컨테이너가 컨테이너 IP 기준 `actuator/health`에서 `200`을 반환할 때까지 대기 후 업스트림을 전환합니다.
- **안전 장치**: 새 컨테이너가 헬스체크 실패 시 기존 `Blue`는 유지한 채 배포를 실패 처리해 가용성 리스크를 낮춥니다.

## 🧩 NPMplus SSE 버퍼링 주의사항

`NPMplus` 템플릿 기준으로 `npmplus_proxy_response_buffering: true`면 Nginx의 `proxy_buffering off`가 적용됩니다.  
즉, 값이 `true`일 때 SSE에서 버퍼링이 꺼집니다.  
⚠️ 오해하지 말 것: **true=켜짐**이 아니라 **true=버퍼링 OFF**입니다.

현재 `deploy.yml`의 Proxy Host 생성/전환 payload는 다음처럼 설정되어 있습니다.

```json
{
  "path": "/sse/",
  "forward_scheme": "http",
  "forward_host": "...",
  "forward_port": 8080,
  "npmplus_proxy_response_buffering": true
}
```

`deploy.yml`에는 `NPMPLUS_SSE_RESPONSE_BUFFERING_OFF: true`로 고정해 두어 `/sse/`가 항상 스트리밍에 맞게 동작하도록 했습니다.
