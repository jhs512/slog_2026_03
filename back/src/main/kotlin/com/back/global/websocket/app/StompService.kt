package com.back.global.websocket.app

import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class StompService(
    private val redisTemplate: StringRedisTemplate,
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
) : MessageListener {
    companion object {
        const val CHANNEL = "stomp-multicast"
    }

    fun send(destination: String, payload: Any) {
        val json = objectMapper.writeValueAsString(
            mapOf("destination" to destination, "payload" to payload)
        )

        try {
            redisTemplate.convertAndSend(CHANNEL, json)
        } catch (_: Exception) {
            messagingTemplate.convertAndSend(destination, payload)
        }
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val node = objectMapper.readTree(message.body)

        val destination = node.get("destination").textValue()!!
        val payload = objectMapper.treeToValue(node.get("payload"), Any::class.java)

        messagingTemplate.convertAndSend(destination, payload)
    }
}
