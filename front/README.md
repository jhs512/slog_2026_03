# front

이 디렉토리는 서비스의 사용자 인터페이스(UI)를 담당하는 **Next.js (App Router)** 기반의 클라이언트 애플리케이션 리포지토리입니다.
백엔드(Spring Boot API 서버)와 통신하여 데이터를 렌더링하고, 사용자 상호작용을 처리합니다.

---

## 🚀 테크 스택

- **프레임워크**: Next.js (App Router)
- **언어**: TypeScript
- **스타일링**: Tailwind CSS
- **컴포넌트**: shadcn/ui 기반 컴포넌트 구성 (`components.json` 설정 기반)
- **API 클라이언트**: `openapi-fetch`를 사용하여 백엔드의 OpenAPI(Swagger) 명세와 강력하게 타입이 연동된 API 호출을 수행합니다.
- **포매팅/린팅**: ESLint, Prettier

---

## 🔌 API 연동 및 인증 전략

이 프로젝트는 백엔드 서버와 완전히 분리되어 있지만, 인증 등 상태 유지 관리는 **쿠키(Cookie)**를 통해 이루어집니다.

- **`openapi-fetch`**: 타입 안정성(`Type-safe`)을 보장하기 위해 사용합니다. 백엔드의 엔드포인트 변경 시 프론트엔드 빌드 단계에서 즉시 타입 에러로 감지할 수 있습니다.
- **인증 (Credentials)**: 모든 API 호출 시 `credentials: "include"` 옵션을 사용합니다. 이로 인해 백엔드가 발급한 `accessToken` 및 `apiKey` HttpOnly 쿠키가 자동으로 백엔드로 전송되며, 프론트엔드 코드 내에서 민감한 토큰을 직접 다루지 않습니다. (stateless 아키텍처 지원)

---

## 🛠️ 로컬 개발 환경 설정

### 1) 환경 변수 세팅

`.env` 파일을 복사하여 자신의 환경에 맞게 수정합니다. (또는 `.env.production` 등을 참고하세요.)

```bash
cp .env.example .env.local
```

_(기본적으로 개발 서버는 `http://localhost:8080` (백엔드)를 바라보도록 설정되어 있어야 합니다.)_

### 2) 패키지 설치

`pnpm` 패키지 매니저를 사용하는 것을 권장합니다 (`pnpm-lock.yaml` 존재).

```bash
pnpm install
```

### 3) 개발 서버 실행

```bash
pnpm dev
```

브라우저에서 `http://localhost:3000`에 접속하여 결과를 확인합니다. 페이지 수정 시 HMR(Hot Module Replacement)에 의해 즉시 뷰가 업데이트됩니다.

---

## 📁 주요 폴더 구조 (예상)

- **`src/app/`**: Next.js App Router의 진입점. 파일 경로 기반의 라우팅 매핑과 레이아웃 설정이 포함되어 있습니다.
- **`src/components/`**: 재사용 가능한 UI 컴포넌트들. (`shadcn/ui` 베이스 컴포넌트 등)
- **`src/global/backend/apiV1/`**: (명세 기반) 자동 또는 수동으로 관리되는 백엔드 API 스키마 및 Fetcher 유틸리티.
- **`public/`**: 폰트, 이미지 등 정적 어셋 폴더.

---

## 📖 더 알아보기

- [Next.js Documentation](https://nextjs.org/docs) - Next.js 기능 및 API 참고
- [Tailwind CSS Documentation](https://tailwindcss.com/docs) - 유틸리티 클래스 참고
- `openapi-fetch` 방식에 대한 상세한 흐름은 백엔드 `back/readme.md` 프론트엔드 연동 챕터(13부)를 함께 참고하세요.
