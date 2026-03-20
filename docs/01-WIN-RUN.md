# Windows 실행 가이드

이 문서는 Windows 환경에서 이 저장소를 로컬 실행할 때 필요한 최소 절차를 정리합니다.

## 1. 기본 전제

- 저장소 경로: `C:\Users\jangk\IdeaProjects\slog_2026_03`
- 셸: PowerShell
- 백엔드 JDK: Java 24
- 패키지 매니저: `pnpm`
- 로컬 의존 서비스: PostgreSQL, Redis

## 2. 프론트 환경 변수

현재 로컬 개발 기본값은 아래와 같습니다.

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_FRONTEND_BASE_URL=http://localhost:3000
```

`front/.env`를 그대로 쓰거나, 필요하면 `front/.env.local`을 만들어 덮어씁니다.

## 3. 백엔드 실행

의존 서비스가 먼저 떠 있어야 합니다.

```powershell
cd C:\Users\jangk\IdeaProjects\slog_2026_03\back\devInfra
docker compose up -d
```

기본 의존 서비스 포트:

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

그 다음 백엔드를 실행합니다.

```powershell
cd C:\Users\jangk\IdeaProjects\slog_2026_03\back
$env:JAVA_HOME='C:\Users\jangk\.jdks\graalvm-jdk-24.0.2'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat bootRun
```

기본 주소:

- 백엔드: `http://localhost:8080`

## 4. 프론트 실행

```powershell
cd C:\Users\jangk\IdeaProjects\slog_2026_03\front
pnpm install
pnpm dev
```

기본 주소:

- 프론트: `http://localhost:3000`

## 5. 테스트 실행

백엔드 테스트 예시:

```powershell
cd C:\Users\jangk\IdeaProjects\slog_2026_03\back
$env:JAVA_HOME='C:\Users\jangk\.jdks\graalvm-jdk-24.0.2'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
```

## 6. 실행 실패 체크리스트

1. `java -version` 이 실행되고 Java 24 계열이 잡히는지 확인합니다.
2. `docker compose up -d` 이후 `docker ps` 에 `redis`, `db` 컨테이너가 떠 있는지 확인합니다.
3. 백엔드가 안 뜨면 `localhost:5432`, `localhost:6379` 포트 충돌이 없는지 확인합니다.
4. 프론트에서 API 호출이 실패하면 `front/.env` 또는 `front/.env.local` 의 `NEXT_PUBLIC_API_BASE_URL` 이 `http://localhost:8080` 인지 확인합니다.
5. 백엔드 테스트가 DB/Redis 연결 오류로 실패하면 `back/devInfra/docker-compose.yml` 상태를 먼저 확인합니다.
6. WebSocket / SSE 연결이 안 되면 백엔드가 먼저 정상 기동했고, 브라우저가 `http://localhost:3000` 에서 열려 있는지 확인합니다.
