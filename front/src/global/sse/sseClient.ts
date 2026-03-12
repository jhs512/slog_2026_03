import { fetchOneTimeToken } from "@/global/auth/oneTimeToken";

const NEXT_PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

const activeConnections = new Map<string, EventSource>();

export interface SseSubscription {
  unsubscribe: () => void;
}

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

  eventSource.onopen = () => {
    console.info(`SSE connected: /sse/${channel}`);
  };

  eventSource.onerror = () => {
    console.error(`SSE error: /sse/${channel}`);
  };

  eventSource.addEventListener("message", (event: MessageEvent) => {
    callback(event.data);
  });

  activeConnections.set(channel, eventSource);

  return {
    unsubscribe: () => {
      eventSource.close();
      activeConnections.delete(channel);
    },
  };
}
