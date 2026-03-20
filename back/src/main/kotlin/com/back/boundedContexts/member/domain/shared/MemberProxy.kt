package com.back.boundedContexts.member.domain.shared

import java.time.Instant

class MemberProxy(
    private val real: Member,
    id: Int,
    username: String,
    nickname: String,
) : Member(id, username, null, nickname) {
    private var useRealState = false

    private fun markUseReal() {
        useRealState = true
    }

    override var nickname: String
        get() = if (useRealState) real.nickname else super.nickname
        set(value) {
            super.nickname = value
            real.nickname = value
        }

    override val name: String
        get() = if (useRealState) real.name else super.name

    override var createdAt: Instant
        get() {
            markUseReal()
            return real.createdAt
        }
        set(value) {
            markUseReal()
            real.createdAt = value
        }

    override var modifiedAt: Instant
        get() {
            markUseReal()
            return real.modifiedAt
        }
        set(value) {
            markUseReal()
            real.modifiedAt = value
        }

    override var profileImgUrl: String
        get() {
            markUseReal()
            return real.profileImgUrl
        }
        set(value) {
            markUseReal()
            real.profileImgUrl = value
        }

    override val profileImgUrlOrDefault: String
        get() {
            markUseReal()
            return real.profileImgUrlOrDefault
        }

    override var apiKey: String
        get() {
            markUseReal()
            return real.apiKey
        }
        set(value) {
            markUseReal()
            real.apiKey = value
        }

    override var password: String?
        get() {
            markUseReal()
            return real.password
        }
        set(value) {
            markUseReal()
            real.password = value
        }
}
