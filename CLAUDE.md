# 프로젝트 메모

## 1) 문서 위치
- 현재 저장소에는 `docs/01-WIN-RUN.md`가 없습니다.
- Windows 실행 가이드가 필요하면 `docs/` 디렉토리를 만들고 아래 경로에 둡니다.
  - `C:\Users\jangk\IdeaProjects\slog_2026_03\docs\01-WIN-RUN.md`

## 2) 백엔드 JDK 기준
- 백엔드 Gradle toolchain은 Java 24를 사용합니다.
  - [`back/build.gradle.kts`](C:/Users/jangk/IdeaProjects/slog_2026_03/back/build.gradle.kts)
  - `JavaLanguageVersion.of(24)`
- 현재 로컬 환경에서 테스트 실행에 사용한 JDK 경로는 아래와 같습니다.
  - `C:\Users\jangk\.jdks\graalvm-jdk-24.0.2`
- `.idea`에 들어 있는 값은 로컬 IDE 상태일 뿐이며, 저장소 기준 진실은 아닙니다.

## 3) 로컬 의존 서비스
- 백엔드 로컬 실행과 통합 테스트에는 PostgreSQL, Redis가 필요합니다.
- 저장소에는 이미 로컬 의존 서비스 실행 파일이 포함되어 있습니다.
  - [`back/devInfra/docker-compose.yml`](C:/Users/jangk/IdeaProjects/slog_2026_03/back/devInfra/docker-compose.yml)
- 백엔드 테스트가 인프라 부족으로 실패하면 `devInfra`를 먼저 띄웁니다.
