package com.back.global.sse.app

import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 범용 SSE 멀티캐스트 서비스.
 *
 * 흐름: send(channel, data) → Redis publish(sse-multicast)
 *      → 모든 인스턴스의 onMessage() → 해당 채널의 SseEmitter들에 전송
 */
@Service
class SseService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : MessageListener {
    companion object {
        const val CHANNEL = "sse-multicast"
        private const val TIMEOUT = 60L * 1000 * 30 // 30분
    }

    private val emitters = ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>()

    fun subscribe(channel: String): SseEmitter {
        val emitter = SseEmitter(TIMEOUT)
        val list = emitters.computeIfAbsent(channel) { CopyOnWriteArrayList() }
        list.add(emitter)

        val remove: () -> Unit = { list.remove(emitter) }
        emitter.onCompletion(remove)
        emitter.onTimeout(remove)
        emitter.onError { remove() }

        emitter.send(SseEmitter.event().name("connect").data("connected"))

        return emitter
    }

    fun send(channel: String, data: Any) {
        val json = objectMapper.writeValueAsString(
            mapOf("channel" to channel, "data" to data)
        )

        try {
            redisTemplate.convertAndSend(CHANNEL, json)
        } catch (_: Exception) {
            dispatch(channel, data)
        }
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        val node = objectMapper.readTree(message.body)
        val channel = node.get("channel").asText()
        val data = objectMapper.treeToValue(node.get("data"), Any::class.java)

        dispatch(channel, data)
    }

    private fun dispatch(channel: String, data: Any) {
        val list = emitters[channel] ?: return
        val jsonData = objectMapper.writeValueAsString(data)

        val dead = mutableListOf<SseEmitter>()

        for (emitter in list) {
            try {
                emitter.send(SseEmitter.event().name("message").data(jsonData, MediaType.APPLICATION_JSON))
            } catch (_: Exception) {
                dead.add(emitter)
            }
        }

        list.removeAll(dead.toSet())
    }
}
