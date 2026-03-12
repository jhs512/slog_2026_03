package com.back.boundedContexts.post.app

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

// @Disabled
class PostSseServiceTest : IntegrationTest() {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var postSseService: PostSseService

    @Autowired
    private lateinit var postFacade: PostFacade

    private fun subscribeSse(channel: String): LinkedBlockingQueue<String> {
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

        return received
    }

    @Test
    fun `notifyNewPost 를 호출하면 posts-new 채널을 구독 중인 클라이언트가 데이터를 수신한다`() {
        val post = postFacade.findById(1)!!
        val received = subscribeSse("posts-new")

        postSseService.notifyNewPost(post)

        val data = received.poll(5, TimeUnit.SECONDS)
        assertThat(data)
            .describedAs("5초 내에 SSE 메시지를 수신해야 함 (Redis publish → dispatch → SSE 전달)")
            .isNotNull()
        assertThat(data).contains(post.id.toString())
        assertThat(data).contains(post.title)
    }
}
