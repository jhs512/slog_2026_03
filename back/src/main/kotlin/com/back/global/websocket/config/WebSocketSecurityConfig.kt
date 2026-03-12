package com.back.global.websocket.config

import com.back.boundedContexts.post.config.PostWebSocketSecurityConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.Message
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager

@Configuration
@EnableWebSocketSecurity
class WebSocketSecurityConfig(
    private val postWebSocketSecurityConfigurer: PostWebSocketSecurityConfigurer,
) {
    @Bean("csrfChannelInterceptor")
    fun noopCsrfChannelInterceptor(): ChannelInterceptor = object : ChannelInterceptor {}

    @Bean
    fun messageAuthorizationManager(
        messages: MessageMatcherDelegatingAuthorizationManager.Builder,
    ): AuthorizationManager<Message<*>> {
        postWebSocketSecurityConfigurer.configure(messages)

        messages.anyMessage().permitAll()

        return messages.build()
    }
}