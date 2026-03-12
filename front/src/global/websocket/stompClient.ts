import { fetchOneTimeToken } from "@/global/auth/oneTimeToken";
import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const NEXT_PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

let stompClient: Client | null = null;
let cachedOneTimeToken: string | null = null;
let authenticatedMode = false;

// 연결 대기 중인 구독 요청들
const pendingSubscriptions: Array<{
  id: string;
  destination: string;
  callback: (message: IMessage) => void;
  resolve: (sub: StompSubscription) => void;
}> = [];

// 활성 구독 목록 (reconnect 시 자동 재구독용)
const activeSubscriptions = new Map<
  string,
  { destination: string; callback: (message: IMessage) => void }
>();

// reconnect 리스너
const reconnectListeners = new Set<() => void>();

let subscriptionIdCounter = 0;

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
    onConnect: () => {
      // 1) 대기 중인 구독 처리
      const pendingIds = new Set<string>();
      while (pendingSubscriptions.length > 0) {
        const pending = pendingSubscriptions.shift()!;
        const sub = stompClient!.subscribe(
          pending.destination,
          pending.callback,
        );
        pending.resolve(sub);
        pendingIds.add(pending.id);
      }

      // 2) 기존 활성 구독 재구독 (reconnect 시, 방금 처리한 pending은 제외)
      for (const [id, entry] of activeSubscriptions.entries()) {
        if (!pendingIds.has(id)) {
          stompClient!.subscribe(entry.destination, entry.callback);
        }
      }

      // 3) reconnect 리스너 호출 (놓친 변경사항 fetch 등)
      for (const listener of reconnectListeners) {
        listener();
      }
    },
  });

  return stompClient;
}

export interface ManagedSubscription {
  unsubscribe: () => void;
}

export function subscribe(
  destination: string,
  callback: (message: IMessage) => void,
  options?: { authenticated?: boolean },
): Promise<ManagedSubscription> {
  const client = getStompClient(options);
  const id = `managed-sub-${++subscriptionIdCounter}`;

  // 활성 구독 목록에 등록
  activeSubscriptions.set(id, { destination, callback });

  const wrapResult = (sub: StompSubscription): ManagedSubscription => ({
    unsubscribe: () => {
      sub.unsubscribe();
      activeSubscriptions.delete(id);
    },
  });

  if (client.connected) {
    const sub = client.subscribe(destination, callback);
    return Promise.resolve(wrapResult(sub));
  }

  // 연결 대기 큐에 추가
  const promise = new Promise<ManagedSubscription>((resolve) => {
    pendingSubscriptions.push({
      id,
      destination,
      callback,
      resolve: (sub) => resolve(wrapResult(sub)),
    });
  });

  if (!client.active) {
    client.activate();
  }

  return promise;
}

export function onReconnect(listener: () => void): () => void {
  reconnectListeners.add(listener);
  return () => reconnectListeners.delete(listener);
}

export function disconnect() {
  if (stompClient?.active) {
    stompClient.deactivate();
  }
}
