package com.back.boundedContexts.member.`in`.shared

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.app.shared.AuthTokenService
import com.back.boundedContexts.member.app.shared.OneTimeTokenService
import com.back.boundedContexts.member.dto.MemberDto
import com.back.boundedContexts.member.dto.MemberWithUsernameDto
import com.back.global.exception.app.AppException
import com.back.global.rsData.RsData
import com.back.global.web.app.Rq
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/member/api/v1/auth")
@Validated
class ApiV1AuthController(
    private val rq: Rq,
    private val memberFacade: MemberFacade,
    private val authTokenService: AuthTokenService,
    private val oneTimeTokenService: OneTimeTokenService,
) {
    data class MemberLoginRequest(
        @field:NotBlank
        @field:Size(min = 2, max = 30)
        val username: String,
        @field:NotBlank
        @field:Size(min = 2, max = 30)
        val password: String,
    )

    data class MemberLoginResBody(
        val item: MemberDto,
        val apiKey: String,
        val accessToken: String,
    )

    @PostMapping("/login")
    @Transactional(readOnly = true)
    fun login(
        @RequestBody @Valid reqBody: MemberLoginRequest,
    ): RsData<MemberLoginResBody> {
        val member = memberFacade.findByUsername(reqBody.username)
            ?: throw AppException("401-1", "존재하지 않는 아이디입니다.")

        memberFacade.checkPassword(member, reqBody.password)

        val accessToken = authTokenService.genAccessToken(member)

        rq.setCookie("apiKey", member.apiKey)
        rq.setCookie("accessToken", accessToken)

        return RsData(
            "200-1",
            "${member.nickname}님 환영합니다.",
            MemberLoginResBody(
                item = MemberDto(member),
                apiKey = member.apiKey,
                accessToken = accessToken,
            )
        )
    }

    @DeleteMapping("/logout")
    fun logout(): RsData<Void> {
        rq.deleteCookie("apiKey")
        rq.deleteCookie("accessToken")

        return RsData("200-1", "로그아웃 되었습니다.")
    }

    data class OneTimeTokenRequest(
        @field:NotBlank
        val allowedPathPrefix: String,
    ) {
        companion object {
            private val ALLOWED_PATH_PREFIXES = setOf("/sse/", "/ws")
        }

        fun validate() {
            if (allowedPathPrefix !in ALLOWED_PATH_PREFIXES) {
                throw AppException("400-1", "허용되지 않은 경로입니다.")
            }
        }
    }

    data class OneTimeTokenResBody(
        val oneTimeToken: String,
    )

    @PostMapping("/oneTimeToken")
    fun oneTimeToken(
        @RequestBody @Valid request: OneTimeTokenRequest,
    ): RsData<OneTimeTokenResBody> {
        request.validate()
        val token = oneTimeTokenService.generate(rq.actor.id, request.allowedPathPrefix)
        return RsData("200-1", "일회용 토큰이 발급되었습니다.", OneTimeTokenResBody(token))
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    fun me(): MemberWithUsernameDto = MemberWithUsernameDto(rq.actor)

}
