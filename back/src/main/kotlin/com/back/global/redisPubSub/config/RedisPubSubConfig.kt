package com.back.global.redisPubSub.config

import com.back.global.sse.app.SseService
import com.back.global.websocket.app.StompService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

@Configuration
class RedisPubSubConfig {
    @Bean
    fun redisMessageListenerContainer(
        redisConnectionFactory: RedisConnectionFactory,
        stompService: StompService,
        sseService: SseService,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(redisConnectionFactory)
        container.addMessageListener(stompService, ChannelTopic(StompService.CHANNEL))
        container.addMessageListener(sseService, ChannelTopic(SseService.CHANNEL))
        return container
    }
}