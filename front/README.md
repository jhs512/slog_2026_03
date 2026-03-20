# 프론트엔드 이해 노트

이 문서는 `front/` 코드를 빠르게 이해하기 위한 강의형 README다.  
설명은 실제 코드 기준으로 진행하고, 백엔드와 맞닿는 계약도 함께 본다.

대상 독자:

- Next.js App Router 기본 개념을 아는 사람
- React 상태 관리와 훅 사용법을 아는 사람
- 이 프로젝트 프론트가 "어디서 무엇을 처리하는지" 빠르게 파악하고 싶은 사람

---

## 강의 구성

1. 이 프론트를 한 문장으로 요약하면
2. 디렉토리 구조를 먼저 읽기
3. `client.ts` — 타입 안전 API 클라이언트
4. `useAuth` / `AuthContext` — 쿠키 인증 상태 관리
5. `withLogin` / `withAdmin` — 페이지 접근 제어
6. 서버 컴포넌트와 클라이언트 컴포넌트 분리
7. `stompClient.ts` — 글 수정 실시간 반영
8. `sseClient.ts` + `useNewPostNotification` — 새 글 알림
9. 글 편집 페이지 — zod + react-hook-form + 단축키
10. 이 프론트에서 배울 수 있는 것

---

# 1강. 이 프론트를 한 문장으로 요약하면

이 프론트는:

> Next.js 16 App Router 기반 클라이언트이며, 백엔드 OpenAPI 스키마를 타입으로 삼아 `openapi-fetch` 로 API를 호출하고, 쿠키 인증 + STOMP/SSE 실시간 연결을 조합한 구조다.

조금 더 실무적으로 말하면:

- 인증 토큰은 프론트 상태에 저장하지 않고 `HttpOnly` 쿠키를 쓴다.
- 페이지 접근 제어는 HOC(`withLogin`, `withAdmin`, `withLogout`)로 처리한다.
- 초기 렌더링과 SEO는 서버 컴포넌트가 맡고, 인터랙션은 클라이언트 컴포넌트가 맡는다.
- 글 수정 반영은 STOMP WebSocket, 새 글 알림은 SSE로 처리한다.
- 백엔드 스펙이 바뀌면 `schema.d.ts` 타입이 먼저 깨져서 프론트 불일치를 빠르게 찾을 수 있다.

---

# 2강. 디렉토리 구조를 먼저 읽기

핵심 구조는 이렇다.

```text
front/src/
├── app/         ← App Router 페이지, 레이아웃, 페이지별 클라이언트 로직
├── components/  ← 재사용 UI, shadcn/ui 베이스 컴포넌트
├── domain/      ← 도메인별 프론트 로직 (예: post 알림)
├── global/      ← 인증, API 클라이언트, SSE, WebSocket, 전역 상태
└── lib/         ← 비즈니스 공통 컴포넌트와 유틸
```

이걸 읽는 기준은 단순하다.

- 페이지라면 `app/`
- 전역 계약/클라이언트/인증이면 `global/`
- 특정 주제의 프론트 로직이면 `domain/`
- UI 조각이면 `components/` 또는 `lib/`

즉 이 프론트는 `pages/` 중심이 아니라 **기능과 역할을 섞어서 나눈 구조**다.

---

# 3강. `client.ts` — 타입 안전 API 클라이언트

가장 먼저 봐야 할 파일은 [`front/src/global/backend/client.ts`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/global/backend/client.ts)다.

```typescript
import type { paths } from "@/global/backend/apiV1/schema";
import createClient from "openapi-fetch";

const NEXT_PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

const client = createClient<paths>({
  baseUrl: NEXT_PUBLIC_API_BASE_URL,
  credentials: "include",
});
```

핵심 포인트:

- `paths` 타입은 백엔드 OpenAPI 스키마에서 생성된 타입이다.
- `openapi-fetch` 는 엔드포인트별 요청/응답 타입을 그대로 추론한다.
- `credentials: "include"` 로 쿠키를 자동 전송한다.

사용 예시는 이런 식이다.

```typescript
const res = await client.GET("/post/api/v1/posts/{id}", {
  params: { path: { id } },
});

if (res.error) {
  toast.error(res.error.msg);
  return;
}

const post = res.data; // PostWithContentDto
```

즉 이 프론트는 문자열 URL을 직접 `fetch` 하지 않고, **백엔드 계약을 타입으로 고정한 API 클라이언트**를 중심으로 돈다.

---

# 4강. `useAuth` / `AuthContext` — 쿠키 인증 상태 관리

인증 상태는 [`front/src/global/auth/hooks/useAuth.tsx`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/global/auth/hooks/useAuth.tsx)에서 관리한다.

```typescript
export default function useAuth() {
  const [loginMember, setLoginMember] = useState<LoginMember>(
    null as unknown as LoginMember,
  );
  const [isPending, setIsPending] = useState(true);
  const isLogin = loginMember !== null;
  const isAdmin = isLogin && loginMember.isAdmin;

  useEffect(() => {
    client.GET("/member/api/v1/auth/me", {}).then((res) => {
      if (res.error) {
        setIsPending(false);
        return;
      }

      setLoginMember(res.data);
      setIsPending(false);
    });
  }, []);
```

마운트 시점에 `/member/api/v1/auth/me` 를 호출해서 현재 로그인 상태를 복원한다.

포인트:

- 토큰은 프론트 상태에 없다.
- 쿠키가 자동 전송되고, 백엔드가 그 쿠키를 해석한다.
- 성공하면 `loginMember`, 실패하면 비로그인 상태로만 본다.

이 상태는 `AuthContext` 로 감싼다.

```typescript
export const AuthContext = createContext<ReturnType<typeof useAuth>>(
  null as unknown as ReturnType<typeof useAuth>,
);

export function AuthProvider({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const authState = useAuth();

  return <AuthContext value={authState}>{children}</AuthContext>;
}

export function useAuthContext() {
  const authState = use(AuthContext);

  if (authState === null) throw new Error("AuthContext is not found");

  return authState;
}
```

React 19의 `use(AuthContext)` 를 쓰는 것도 특징이다.

---

# 5강. `withLogin` / `withAdmin` — 페이지 접근 제어

페이지 접근 제어는 HOC로 처리한다.

예를 들어 [`front/src/global/auth/hoc/withLogin.tsx`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/global/auth/hoc/withLogin.tsx):

```typescript
export default function withLogin<P extends object>(
  Component: React.ComponentType<P>,
) {
  return function WithLoginComponent(props: P) {
    const { isLogin } = useAuthContext();

    if (!isLogin) {
      return (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground gap-2">
          <span className="text-lg font-medium">로그인 후 이용해주세요.</span>
        </div>
      );
    }

    return <Component {...props} />;
  };
}
```

비슷하게:

- `withLogin` → 로그인 필요
- `withAdmin` → 로그인 + 관리자 필요
- `withLogout` → 로그인한 사용자는 접근 불가

이 방식의 장점은 페이지 진입 조건이 컴포넌트 선언부에서 바로 보인다는 점이다.

---

# 6강. 서버 컴포넌트와 클라이언트 컴포넌트 분리

글 상세 페이지는 이 구조가 잘 드러난다.

서버 컴포넌트 [`front/src/app/p/[id]/page.tsx`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/app/p/[id]/page.tsx):

```typescript
async function getPost(id: number) {
  const res = await client.GET("/post/api/v1/posts/{id}", {
    params: {
      path: {
        id,
      },
    },
    headers: {
      cookie: (await cookies()).toString(),
    },
  });

  return res;
}

export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const postResponse = await getPost(parseInt(id));

  if (postResponse.error) {
    return (
      <div className="flex-1 flex items-center justify-center">
        {postResponse.error.msg}
      </div>
    );
  }

  return <ClientPage initialPost={postResponse.data} />;
}
```

클라이언트 컴포넌트 [`front/src/app/p/[id]/ClientPage.tsx`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/app/p/[id]/ClientPage.tsx):

```typescript
"use client";

export default function ClientPage({
  initialPost,
}: {
  initialPost: PostWithContentDto;
}) {
  const postState = usePostClient(initialPost);
  const postCommentsState = usePostComments(initialPost.id);

  return (
    <div className="container mx-auto px-4 py-6">
      <PostInfo postState={postState} />

      <div className="mt-8 border-t dark:border-gray-700 pt-8">
        <PostCommentWriteAndList postCommentsState={postCommentsState} />
      </div>
    </div>
  );
}
```

핵심은:

- 서버 컴포넌트는 초기 데이터/SEO 담당
- 클라이언트 컴포넌트는 좋아요, 댓글, 실시간 반영 담당

즉 **초기 렌더링은 서버, 인터랙션은 클라이언트**다.

---

# 7강. `stompClient.ts` — 글 수정 실시간 반영

STOMP 클라이언트는 [`front/src/global/websocket/stompClient.ts`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/global/websocket/stompClient.ts)다.

```typescript
export function getStompClient(options?: { authenticated?: boolean }): Client {
  authenticatedMode = options?.authenticated ?? false;
  if (stompClient) return stompClient;

  stompClient = new Client({
    webSocketFactory: () => {
      const base = `${NEXT_PUBLIC_API_BASE_URL}/ws`;
      const url = cachedOneTimeToken
        ? `${base}?oneTimeToken=${cachedOneTimeToken}`
        : base;
      cachedOneTimeToken = null;
      return new SockJS(url);
    },
    beforeConnect: async () => {
      cachedOneTimeToken = authenticatedMode
        ? await fetchOneTimeToken("/ws")
        : null;
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  return stompClient;
}
```

읽는 포인트:

- `/ws` 연결 자체는 SockJS로 연다.
- 인증이 필요하면 연결 직전에 `oneTimeToken` 을 발급받는다.
- `stompClient` 는 싱글톤이라 페이지 이동에도 연결을 재사용한다.

구독 함수는 active/pending 구독을 관리해서 reconnect 후 자동 복구가 가능하다.

실제 소비 지점은 `usePostClient` 쪽이다. 글 상세 페이지는 `/topic/posts/{id}/modified` 를 구독해서 수정 내용을 실시간 반영한다.

---

# 8강. `sseClient.ts` + `useNewPostNotification` — 새 글 알림

SSE는 [`front/src/global/sse/sseClient.ts`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/global/sse/sseClient.ts)를 쓴다.

```typescript
export async function subscribe(
  channel: string,
  callback: (data: string) => void,
  options?: { authenticated?: boolean },
): Promise<SseSubscription> {
  const token = options?.authenticated
    ? await fetchOneTimeToken("/sse/")
    : null;
  const base = `${NEXT_PUBLIC_API_BASE_URL}/sse/${channel}`;
  const url = token ? `${base}?oneTimeToken=${token}` : base;

  const eventSource = new EventSource(url);
```

글 알림 훅 [`front/src/domain/post/hooks/useNewPostNotification.ts`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/domain/post/hooks/useNewPostNotification.ts):

```typescript
export function useNewPostNotification(
  onNewPost?: (post: PostNotification) => void,
) {
  const [latestPost, setLatestPost] = useState<PostNotification | null>(null);
  const { isLogin, isPending } = useAuthContext();

  useEffect(() => {
    if (isPending) return;

    let cancelled = false;
    let subscription: Awaited<ReturnType<typeof subscribe>> | null = null;

    subscribe(
      "posts-new",
      (data) => {
        const post: PostNotification = JSON.parse(data);
        setLatestPost(post);
        onNewPost?.(post);
      },
      { authenticated: isLogin },
    ).then((sub) => {
      if (cancelled) sub.unsubscribe();
      else subscription = sub;
    });

    return () => {
      cancelled = true;
      subscription?.unsubscribe();
    };
  }, [isPending, isLogin]);
}
```

즉:

- 글 수정은 STOMP
- 새 글 알림은 SSE

실시간 요구를 둘로 나눠서 처리한다.

---

# 9강. 글 편집 페이지 — zod + react-hook-form + Cmd+S

편집 페이지 [`front/src/app/p/[id]/edit/page.tsx`](C:/Users/jangk/IdeaProjects/slog_2026_03/front/src/app/p/[id]/edit/page.tsx)는 이 프론트에서 가장 밀도가 높은 파일 중 하나다.

포인트는 세 가지다.

## 1. 로그인 보호

```typescript
export default withLogin(function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id: idStr } = use(params);
  const id = parseInt(idStr);
  // ...
});
```

## 2. 조건부 유효성 검사

```typescript
const postFormSchema = z
  .object({
    title: z.string().max(100, "제목은 100자 이하여야 합니다."),
    content: z.string(),
    published: z.boolean(),
    listed: z.boolean(),
  })
  .refine((data) => {
    if (data.published || data.title.trim().length > 0) {
      return data.title.trim().length >= 2;
    }
    return true;
  }, {
    message: "제목은 2자 이상이어야 합니다.",
    path: ["title"],
  });
```

임시저장과 공개 글의 검증 조건이 다르다.

## 3. published / listed 연동

```typescript
<Checkbox
  id="published"
  checked={field.value}
  onCheckedChange={(checked) => {
    field.onChange(checked);
    if (!checked) form.setValue("listed", false);
  }}
/>
```

백엔드 도메인의 불변 규칙을 프론트에서도 미리 반영한다.

그리고 저장 단축키도 있다.

```typescript
useEffect(() => {
  const handleKeyDown = (e: KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && (e.key === "s" || e.code === "KeyS")) {
      e.preventDefault();
      form.handleSubmit(onSubmit)();
    }
  };

  window.addEventListener("keydown", handleKeyDown);
  return () => window.removeEventListener("keydown", handleKeyDown);
}, [form, onSubmit]);
```

---

# 10강. 이 프론트에서 배울 수 있는 것

- App Router 서버/클라이언트 컴포넌트 분리
- OpenAPI 스키마 기반 타입 안전 API 클라이언트
- 쿠키 인증 상태를 프론트 상태와 분리하는 패턴
- HOC 기반 페이지 접근 제어
- STOMP와 SSE를 역할별로 나눠 쓰는 실시간 설계
- zod + react-hook-form 조건부 유효성 검사
- 단축키와 URL 파라미터 기반 UX 처리

---

## 결론

이 프론트는 단순 화면 모음이 아니라, **백엔드 계약을 타입으로 고정하고, 인증과 실시간 연결을 일관된 패턴으로 다루는 구조**다.

페이지를 읽을 때는:

1. 서버/클라이언트가 어디서 갈리는지
2. 인증 상태를 어디서 얻는지
3. API 타입을 어디서 보장하는지
4. 실시간 반영이 STOMP인지 SSE인지

이 네 가지를 먼저 보면 길을 잃지 않는다.
