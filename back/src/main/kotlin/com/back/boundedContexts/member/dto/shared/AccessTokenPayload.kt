package com.back.boundedContexts.member.dto.shared

data class AccessTokenPayload(
    val id: Int,
    val username: String,
    val name: String,
)
