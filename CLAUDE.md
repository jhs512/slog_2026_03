# Project Notes

## 1) Win run docs location
- `docs/01-WIN-RUN.md` is currently not present in this repository.
- If you add it, place it at:
  - `C:\Users\jangk\IdeaProjects\slog_2026_03\docs\01-WIN-RUN.md`

## 2) IntelliJ Gradle JDK (JDK 24) reference
- From `.idea` settings:
  - `back/.idea/misc.xml`
    - `project-jdk-name="graalvm-jdk-24"`
    - `languageLevel="JDK_24"`
  - `back/.idea/workspace.xml`
    - `javaHome="C:\Users\jangk\.jdks\graalvm-jdk-24.0.2"`
- This matches the repo's Kotlin/Gradle target version 24 in:
  - `back/build.gradle.kts` (`JavaLanguageVersion.of(24)`).

## 3) Local dependency services
- Turn on `devInfra` before running backend tests in this repo.
