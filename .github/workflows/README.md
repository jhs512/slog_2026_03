# GitHub Actions 배포 노트

이 문서는 `.github/workflows/` 아래 배포 자동화를 빠르게 이해하기 위한 강의형 README다.  
설명은 실제 [`deploy.yml`](C:/Users/jangk/IdeaProjects/slog_2026_03/.github/workflows/deploy.yml) 기준으로 진행한다.

대상 독자:

- GitHub Actions 기본 문법을 아는 사람
- Docker build/push 흐름을 아는 사람
- 이 프로젝트 배포가 "어떤 순서로, 어떤 가정 위에서" 돌아가는지 알고 싶은 사람

---

## 강의 구성

1. 이 워크플로우를 한 문장으로 요약하면
2. 언제 실행되는가
3. 전역 환경 변수는 무엇을 뜻하는가
4. Job 1 — 다음 태그 계산
5. Job 2 — Docker 이미지 빌드와 GHCR 푸시
6. Job 3 — SSM 기반 Blue/Green 무중단 배포
7. Job 4 — 실제 태그/릴리스 생성
8. GitHub Secrets는 무엇이 필요한가
9. 이 워크플로우에서 배울 수 있는 것

---

# 1강. 이 워크플로우를 한 문장으로 요약하면

이 워크플로우는:

> `main` 브랜치의 백엔드 관련 변경을 감지하면, 버전 태그를 미리 계산하고, Docker 이미지를 GHCR에 올린 뒤, AWS SSM으로 EC2에서 Blue/Green 스위치를 수행하고, 마지막에 태그와 릴리스를 만드는 파이프라인이다.

즉 단계로 풀면:

1. 다음 버전 계산
2. 이미지 빌드/푸시
3. EC2 원격 배포
4. 태그/릴리스 마무리

---

# 2강. 언제 실행되는가

트리거는 [`deploy.yml`](C:/Users/jangk/IdeaProjects/slog_2026_03/.github/workflows/deploy.yml)의 `on.push`다.

핵심 조건:

- 브랜치: `main`
- 경로:
  - `.github/workflows/*.yml`
  - `.github/workflows/*.yaml`
  - `back/.env`
  - `back/src/**`
  - `back/build.gradle.kts`
  - `back/settings.gradle.kts`
  - `back/Dockerfile`

즉 프론트 전용 변경에는 반응하지 않고, **백엔드 또는 배포 파이프라인 변경**에만 반응한다.

---

# 3강. 전역 환경 변수는 무엇을 뜻하는가

워크플로우 상단 `env:` 블록은 배포 스크립트 전체의 전제다.

예를 들면:

- `IMAGE_REPOSITORY`
  - GHCR 이미지 이름
- `CONTAINER_1_NAME`, `CONTAINER_2_NAME`
  - Blue/Green 슬롯 이름
- `CONTAINER_PORT`
  - 컨테이너 내부 포트 (`8080`)
- `EC2_INSTANCE_TAG_NAME`
  - 배포 대상 EC2를 찾는 Name 태그
- `DOCKER_NETWORK`
  - EC2 안 Docker 네트워크 이름 (`common`)
- `BACK_DIR`
  - Docker build context (`back`)
- `NPM_BASE_URL`
  - NPMplus Admin API (`https://127.0.0.1:81`)

즉 이 워크플로우는 “어느 서버에, 어떤 컨테이너 이름으로, 어떤 포트 구조로 배포할 것인가”를 전역 변수로 고정해둔 구조다.

---

# 4강. Job 1 — 다음 태그 계산

첫 번째 job은 `calculateTag` 다.

```yaml
calculateTag:
  runs-on: ubuntu-latest
  outputs:
    tag_name: ${{ steps.dry_run.outputs.new_tag }}
  steps:
    - uses: actions/checkout@v6

    - name: 다음 버전 태그 계산 (dry-run)
      id: dry_run
      uses: mathieudutour/github-tag-action@v6.2
      with:
        github_token: ${{ secrets.GITHUB_TOKEN }}
        dry_run: true
```

핵심은:

- 실제 태그를 아직 만들지 않음
- “이번 배포가 성공하면 무슨 태그가 될까”만 미리 계산
- 그 결과를 다음 job들이 공유

이렇게 해야 이미지 태그와 최종 릴리스 태그를 같은 값으로 맞출 수 있다.

---

# 5강. Job 2 — Docker 이미지 빌드와 GHCR 푸시

두 번째 job은 `buildImageAndPush` 다.

핵심 흐름:

1. 저장소 checkout
2. Buildx 설치
3. GHCR 로그인
4. repository owner를 소문자로 정규화
5. `back/` 기준 Docker build
6. `latest` + 계산된 버전 태그 둘 다 push

중요한 부분은 태그 두 개를 동시에 올린다는 점이다.

```yaml
tags: |
  ghcr.io/${{ env.OWNER_LC }}/${{ env.IMAGE_REPOSITORY }}:${{ needs.calculateTag.outputs.tag_name }}
  ghcr.io/${{ env.OWNER_LC }}/${{ env.IMAGE_REPOSITORY }}:latest
```

이렇게 하면:

- 사람이 보기 쉬운 `latest`
- 배포 이력을 추적할 수 있는 고정 버전 태그

를 동시에 유지할 수 있다.

---

# 6강. Job 3 — SSM 기반 Blue/Green 무중단 배포

세 번째 job `deploy`가 진짜 핵심이다.

구조를 단순화하면:

```text
GitHub Actions
  -> AWS 자격 구성
  -> Name 태그로 EC2 조회
  -> .env를 base64로 인코딩
  -> SSM으로 원격 스크립트 실행
       -> NPMplus 로그인
       -> 현재 upstream 확인
       -> Blue / Green 역할 계산
       -> Green 컨테이너 기동
       -> /actuator/health 확인
       -> Proxy Host upstream 전환
       -> 이전 Blue 정리
       -> 오래된 이미지 정리
```

## 1. 왜 SSM인가

이 워크플로우는 SSH가 아니라 SSM을 쓴다.

의도는 명확하다.

- SSH 키 배포 부담 감소
- EC2에 대한 표준 원격 실행 경로 확보
- GitHub Actions에서 동기식으로 로그 수집 가능

## 2. 왜 Blue/Green인가

슬롯 이름은 고정이다.

- `slog_1_1`
- `slog_1_2`

하지만 “blue/green” 역할은 매 배포마다 바뀐다.

- 현재 운영 중인 쪽 = blue
- 다음 후보 = green

즉 blue/green은 이름이 아니라 **역할**이다.

## 3. 왜 host port 매핑을 안 쓰는가

배포 스크립트는 컨테이너를 이런 식으로 띄운다.

```bash
docker run -d --name "${GREEN}" \
  --restart unless-stopped \
  --network "${NET}" \
  -e TZ=Asia/Seoul \
  -v /tmp/.env:/app/.env:ro \
  "${IMAGE}"
```

호스트 포트를 직접 publish하지 않는다.  
대신 NPMplus가 같은 Docker 네트워크 `common` 안에서 컨테이너 이름 + 내부 포트로 프록시한다.

이 방식의 장점:

- blue, green 동시 기동 가능
- 포트 충돌 없음
- 프록시 전환만으로 트래픽 스위치 가능

## 4. 헬스체크

Green은 먼저 뜨고, `/actuator/health` 가 `200` 이 될 때까지 기다린다.

```bash
CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://${CONTAINER_IP}:${PORT_IN}/actuator/health" || echo 000)
```

즉 “컨테이너가 떴다”가 아니라 “애플리케이션이 응답할 준비가 됐다”를 기준으로 전환한다.

## 5. NPMplus SSE 버퍼링

SSE는 `/sse/` location을 별도 설정한다.

```json
{
  "path": "/sse/",
  "forward_scheme": "http",
  "forward_host": "...",
  "forward_port": 8080,
  "npmplus_proxy_response_buffering": true
}
```

주의:

- 이름만 보면 `true=버퍼링 ON` 같지만
- 이 템플릿에선 `true`일 때 실제로 `proxy_buffering off`

즉 **SSE를 스트리밍답게 흘리기 위한 설정**이다.

---

# 7강. Job 4 — 실제 태그/릴리스 생성

배포가 성공하면 마지막 job `makeTagAndRelease` 가 돈다.

```yaml
makeTagAndRelease:
  runs-on: ubuntu-latest
  needs: deploy
```

흐름:

1. checkout
2. `github-tag-action`으로 실제 태그 생성
3. `softprops/action-gh-release`로 GitHub Release 생성

포인트는 “배포 성공 후에만 태그를 확정한다”는 점이다.

즉:

- 계산만 한 태그
- 실제 반영된 태그

를 분리해서 실패한 배포가 릴리스 기록으로 남지 않게 한다.

---

# 8강. GitHub Secrets는 무엇이 필요한가

최소한 이 값들이 필요하다.

- `AWS_REGION`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `DOT_ENV`

또한 EC2 쪽 `/etc/environment`에는 이런 값이 이미 있어야 한다.

- `PASSWORD_1`
- `APP_1_DOMAIN`

즉 GitHub Secrets와 EC2 부트스트랩 환경변수 둘 다 맞아야 배포가 돈다.

---

# 9강. 이 워크플로우에서 배울 수 있는 것

- GitHub Actions job 분리와 output 전달
- dry-run 태그 계산 후 실제 태그 확정하는 방식
- GHCR 이중 태그 전략 (`latest` + version)
- AWS SSM을 이용한 원격 배포
- Docker 네트워크 기반 Blue/Green 스위치
- NPMplus API로 프록시를 코드에서 전환하는 방식

---

## 결론

이 워크플로우는 단순히 “이미지 빌드 후 서버에 올리기”가 아니다.  
**태그 계산, 이미지 생성, 헬스체크, 프록시 스위치, 릴리스 기록**까지 하나의 흐름으로 묶어둔 배포 자동화다.

읽을 때는:

1. 언제 실행되는가
2. 어떤 이미지 태그를 쓰는가
3. EC2를 어떻게 찾는가
4. Blue/Green 전환이 어디서 일어나는가

이 네 가지만 먼저 보면 전체 구조가 잡힌다.
