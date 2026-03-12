"use client";

import { useEffect, useRef, useState } from "react";

import { useAuthContext } from "@/global/auth/hooks/useAuth";
import { subscribe } from "@/global/sse/sseClient";

export interface PostNotification {
  id: number;
  title: string;
  authorId: number;
  authorName: string;
  authorProfileImgUrl: string;
  createdAt: string;
}

export function useNewPostNotification(
  onNewPost?: (post: PostNotification) => void,
) {
  const [latestPost, setLatestPost] = useState<PostNotification | null>(null);
  const { isLogin, isPending } = useAuthContext();
  const callbackRef = useRef(onNewPost);

  useEffect(() => {
    callbackRef.current = onNewPost;
  }, [onNewPost]);

  useEffect(() => {
    if (isPending) return;

    let cancelled = false;
    let subscription: Awaited<ReturnType<typeof subscribe>> | null = null;

    subscribe(
      "posts-new",
      (data) => {
        const post: PostNotification = JSON.parse(data);

        setLatestPost(post);
        callbackRef.current?.(post);
      },
      { authenticated: isLogin },
    ).then((sub) => {
      if (cancelled) {
        sub.unsubscribe();
      } else {
        subscription = sub;
      }
    });

    return () => {
      cancelled = true;
      subscription?.unsubscribe();
    };
  }, [isPending, isLogin]);

  return { latestPost };
}
