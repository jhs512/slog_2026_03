package com.back.boundedContexts.post.app

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.dto.PostWithContentDto
import com.back.global.websocket.app.StompService
import org.springframework.stereotype.Service

@Service
class PostStompService(
    private val stompService: StompService,
) {
    fun notifyPostModified(post: Post) {
        stompService.send("/topic/posts/${post.id}/modified", PostWithContentDto(post))
    }

}
