"use client";

import { useEffect } from "react";

import type { components } from "@/global/backend/apiV1/schema";

import PostCommentWriteAndList from "./_components/PostCommentWriteAndList";
import PostInfo from "./_components/PostInfo";
import usePostClient from "./_hooks/usePostClient";
import usePostComments from "./_hooks/usePostComments";

type PostWithContentDto = components["schemas"]["PostWithContentDto"];

export default function ClientPage({
  initialPost,
}: {
  initialPost: PostWithContentDto;
}) {
  const postState = usePostClient(initialPost);
  const postCommentsState = usePostComments(initialPost.id);

  useEffect(() => {
    const hash = window.location.hash;
    if (!hash) return;

    const id = decodeURIComponent(hash.slice(1));
    const timer = setTimeout(() => {
      const el =
        document.getElementById(id) ??
        document.querySelector(`[id^="${CSS.escape(id)}"]`);
      el?.scrollIntoView({ behavior: "smooth" });
    }, 300);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="container mx-auto px-4 py-6">
      <PostInfo postState={postState} />

      <div className="mt-8 border-t dark:border-gray-700 pt-8">
        <PostCommentWriteAndList postCommentsState={postCommentsState} />
      </div>
    </div>
  );
}
