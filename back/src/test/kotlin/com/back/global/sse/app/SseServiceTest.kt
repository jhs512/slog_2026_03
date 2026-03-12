package com.back.global.sse.app

import com.back.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class SseServiceTest : IntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var sseService: SseService

    private fun subscribeSse(channel: String): Pair<LinkedBlockingQueue<String>, Thread> {
        val received = LinkedBlockingQueue<String>()

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI("http://localhost:$port/sse/$channel"))
            .header("Accept", "text/event-stream")
            .GET()
            .build()

        val thread = Thread {
            try {
                client.send(request, HttpResponse.BodyHandlers.ofLines()).body().forEach { line ->
                    if (line.startsWith("data:")) {
                        received.put(line.removePrefix("data:"))
                    }
                }
            } catch (_: Exception) {
            }
        }
        thread.isDaemon = true
        thread.start()

        // connect 이벤트 수신 대기
        val connectData = received.poll(5, TimeUnit.SECONDS)
        assertThat(connectData).describedAs("connect 이벤트를 수신해야 함").isNotNull()

        return received to thread
    }

    @Test
    fun `send 를 호출하면 해당 채널을 구독 중인 클라이언트가 데이터를 수신한다`() {
        val (received, _) = subscribeSse("test-channel")

        sseService.send("test-channel", mapOf("msg" to "hello"))

        val data = received.poll(5, TimeUnit.SECONDS)
        assertThat(data)
            .describedAs("5초 내에 메시지를 수신해야 함 (Redis publish → dispatch → SSE 전달)")
            .isNotNull()
        assertThat(data).contains("hello")
    }

    @Test
    fun `다른 채널에 send 하면 수신하지 않는다`() {
        val (received, _) = subscribeSse("channel-a")

        sseService.send("channel-b", mapOf("msg" to "should not receive"))

        val data = received.poll(2, TimeUnit.SECONDS)
        assertThat(data).isNull()
    }
}
