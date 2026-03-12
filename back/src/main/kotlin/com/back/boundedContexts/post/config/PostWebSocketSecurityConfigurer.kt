package com.back.boundedContexts.post.config

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.post.app.PostFacade
import com.back.global.security.domain.SecurityUser
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.core.Authentication
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager
import org.springframework.stereotype.Component
import java.util.function.Supplier

@Component
class PostWebSocketSecurityConfigurer(
    @Lazy private val postFacade: PostFacade,
    private val actorFacade: ActorFacade,
) {
    private val postTopicPattern = Regex("^/topic/posts/(\\d+)/modified$")

    fun configure(messages: MessageMatcherDelegatingAuthorizationManager.Builder) {
        messages
            .simpSubscribeDestMatchers("/topic/posts/*/modified")
            .access { auth, ctx -> canSubscribeToPostModified(auth, ctx) }
    }

    private fun canSubscribeToPostModified(
        authentication: Supplier<out Authentication?>,
        context: MessageAuthorizationContext<*>,
    ): AuthorizationDecision {
        val destination = SimpMessageHeaderAccessor.getDestination(context.message.headers)
            ?: return AuthorizationDecision(false)

        val postId = postTopicPattern.find(destination)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?: return AuthorizationDecision(true)

        val post = postFacade.findById(postId)
            ?: return AuthorizationDecision(true)

        if (post.published) return AuthorizationDecision(true)

        val securityUser = authentication.get()?.principal as? SecurityUser
            ?: return AuthorizationDecision(false)
        return AuthorizationDecision(post.canRead(actorFacade.memberOf(securityUser)))
    }
}
