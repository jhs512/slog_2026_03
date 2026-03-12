package com.back.boundedContexts.post.app

import com.back.boundedContexts.post.domain.Post
import com.back.global.sse.app.SseService
import org.springframework.stereotype.Service

@Service
class PostSseService(
    private val sseService: SseService,
) {
    fun notifyNewPost(post: Post) {
        sseService.send(
            "posts-new",
            mapOf(
                "id" to post.id,
                "title" to post.title,
                "authorId" to post.author.id,
                "authorName" to post.author.nickname,
                "authorProfileImgUrl" to post.author.redirectToProfileImgUrlOrDefault,
                "createdAt" to post.createdAt.toString(),
            )
        )
    }
}
