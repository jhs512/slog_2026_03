# front

이 디렉토리는 서비스의 사용자 인터페이스(UI)를 담당하는 **Next.js (App Router)** 기반의 클라이언트 애플리케이션입니다.
백엔드(Spring Boot API 서버)와 통신하여 데이터를 렌더링하고, 인증 상태/실시간 연결/UI 상호작용을 처리합니다.

---

## 🚀 테크 스택

- **프레임워크**: Next.js (App Router)
- **언어**: TypeScript
- **스타일링**: Tailwind CSS
- **컴포넌트**: shadcn/ui (`components.json`, New York 스타일)
- **API 클라이언트**: `openapi-fetch`를 사용하여 백엔드의 OpenAPI(Swagger) 명세와 강력하게 타입이 연동된 API 호출을 수행합니다.
- **실시간 통신**: STOMP + SockJS, SSE
- **포매팅/린팅**: ESLint, Prettier

---

## 🔌 API 연동 및 인증 전략

이 프로젝트는 백엔드 서버와 완전히 분리되어 있지만, 인증 등 상태 유지 관리는 **쿠키(Cookie)**를 통해 이루어집니다.

- **`openapi-fetch`**: 타입 안정성(`Type-safe`)을 보장하기 위해 사용합니다. 백엔드의 엔드포인트 변경 시 프론트엔드 빌드 단계에서 즉시 타입 에러로 감지할 수 있습니다.
- **인증 (Credentials)**: 모든 API 호출 시 `credentials: "include"` 옵션을 사용합니다. 이로 인해 백엔드가 발급한 `accessToken` 및 `apiKey` HttpOnly 쿠키가 자동으로 백엔드로 전송되며, 프론트엔드 코드 내에서 민감한 토큰을 직접 다루지 않습니다. (stateless 아키텍처 지원)
- **인증 UI 패턴**: `global/auth/hooks/useAuth.tsx` 의 컨텍스트를 중심으로 `withLogin`, `withAdmin`, `withLogout` HOC가 페이지 접근을 제어합니다.
- **실시간 인증**: STOMP(`/ws`)와 SSE(`/sse/`) 연결은 `oneTimeToken` 을 발급받아 연결 시점에만 사용합니다.

---

## 🛠️ 로컬 개발 환경 설정

### 1) 환경 변수 세팅

현재 저장소에는 `.env`, `.env.production` 이 있으며, 필요하면 `.env.local` 을 별도로 만들어 덮어쓸 수 있습니다.

```bash
cp .env .env.local
```

`NEXT_PUBLIC_API_BASE_URL` 이 개발 백엔드 주소를 가리키도록 맞춰야 합니다. 일반적인 로컬 개발 기준은 `http://localhost:8080` 입니다.

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

## 📁 주요 폴더 구조

- **`src/app/`**: Next.js App Router 페이지, 레이아웃, 페이지별 클라이언트 로직.
- **`src/components/`**: 범용 UI 컴포넌트와 shadcn/ui 베이스 컴포넌트.
- **`src/domain/`**: 게시글 알림 등 도메인별 프론트 로직.
- **`src/global/`**: 인증, 백엔드 클라이언트, SSE, WebSocket, 전역 상태.
- **`src/lib/`**: 에디터/헤더 등 비즈니스 성격의 공통 컴포넌트와 유틸.
- **`public/`**: 정적 어셋 폴더.

--- 

## 🔄 타입/실시간 흐름 요약

- **API 타입**: `src/global/backend/apiV1/schema.d.ts` 의 OpenAPI 타입을 `openapi-fetch` 클라이언트가 직접 사용합니다.
- **글 수정 반영**: `src/global/websocket/stompClient.ts` 를 통해 `/topic/posts/{id}/modified` 를 구독합니다.
- **새 글 알림**: `src/global/sse/sseClient.ts` 와 `src/domain/post/hooks/useNewPostNotification.ts` 가 `posts-new` SSE 채널을 구독합니다.
- **로그인 상태**: `useAuthContext()` 가 `loginMember`, `isLogin`, `isAdmin`, `logout` 등을 제공합니다.

---

## 📖 더 알아보기

- [Next.js Documentation](https://nextjs.org/docs) - Next.js 기능 및 API 참고
- [Tailwind CSS Documentation](https://tailwindcss.com/docs) - 유틸리티 클래스 참고
- `openapi-fetch` 및 실시간 연동의 상세 흐름은 [back/README.md](C:/Users/jangk/IdeaProjects/slog_2026_03/back/README.md)의 프론트엔드 관련 챕터를 함께 참고하세요.
