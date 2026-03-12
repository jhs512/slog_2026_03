package com.back.global.sse.`in`

import com.back.global.sse.app.SseService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/sse")
class SseController(
    private val sseService: SseService,
) {
    @GetMapping("/{channel}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(@PathVariable channel: String): SseEmitter {
        return sseService.subscribe(channel)
    }
}
