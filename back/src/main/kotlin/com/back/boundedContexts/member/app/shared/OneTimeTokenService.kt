package com.back.boundedContexts.member.app.shared

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*

@Service
class OneTimeTokenService(
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val KEY_PREFIX = "one-time-token:"
        private val TTL: Duration = Duration.ofSeconds(30)
    }

    fun generate(memberId: Int, allowedPathPrefix: String): String {
        val token = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set("$KEY_PREFIX$token", "$memberId:$allowedPathPrefix", TTL)
        return token
    }

    fun validate(token: String, requestUri: String): Int? {
        val key = "$KEY_PREFIX$token"
        val value = redisTemplate.opsForValue().get(key) ?: return null
        val (memberIdStr, allowedPathPrefix) = value.split(":", limit = 2)
        if (!requestUri.startsWith(allowedPathPrefix)) return null
        return memberIdStr.toIntOrNull()
    }
}
