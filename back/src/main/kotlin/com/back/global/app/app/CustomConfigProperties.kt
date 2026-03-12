package com.back.global.app.app

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom")
class CustomConfigProperties(
    val notProdMembers: List<NotProdMember>
) {
    data class NotProdMember(val username: String, val nickname: String, val profileImgUrl: String) {
        fun apiKey(): String {
            return username
        }
    }
}