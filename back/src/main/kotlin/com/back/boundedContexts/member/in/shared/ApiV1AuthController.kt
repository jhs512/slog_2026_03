package com.back.boundedContexts.member.`in`.shared

import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.app.shared.AuthTokenService
import com.back.boundedContexts.member.app.shared.OneTimeTokenService
import com.back.boundedContexts.member.dto.MemberDto
import com.back.boundedContexts.member.dto.MemberWithUsernameDto
import com.back.global.app.app.AppFacade
import com.back.global.exception.app.AppException
import com.back.global.rsData.RsData
import com.back.global.security.domain.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/member/api/v1/auth")
@Validated
class ApiV1AuthController(
    private val memberFacade: MemberFacade,
    private val actorFacade: ActorFacade,
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
        response: HttpServletResponse,
    ): RsData<MemberLoginResBody> {
        val member = memberFacade.findByUsername(reqBody.username)
            ?: throw AppException("401-1", "존재하지 않는 아이디입니다.")

        memberFacade.checkPassword(member, reqBody.password)

        val accessToken = authTokenService.genAccessToken(member)

        response.addAuthCookie("apiKey", member.apiKey)
        response.addAuthCookie("accessToken", accessToken)

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
    fun logout(
        response: HttpServletResponse,
    ): RsData<Void> {
        response.expireAuthCookie("apiKey")
        response.expireAuthCookie("accessToken")

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
        @AuthenticationPrincipal securityUser: SecurityUser,
        @RequestBody @Valid request: OneTimeTokenRequest,
    ): RsData<OneTimeTokenResBody> {
        request.validate()
        val token = oneTimeTokenService.generate(securityUser.id, request.allowedPathPrefix)
        return RsData("200-1", "일회용 토큰이 발급되었습니다.", OneTimeTokenResBody(token))
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    fun me(
        @AuthenticationPrincipal securityUser: SecurityUser,
    ): MemberWithUsernameDto = MemberWithUsernameDto(actorFacade.memberOf(securityUser))

    private fun HttpServletResponse.addAuthCookie(name: String, value: String) {
        addCookie(Cookie(name, value).apply {
            path = "/"
            isHttpOnly = true
            domain = AppFacade.siteCookieDomain
            secure = true
            setAttribute("SameSite", "Strict")
            maxAge = 60 * 60 * 24 * 365
        })
    }

    private fun HttpServletResponse.expireAuthCookie(name: String) {
        addCookie(Cookie(name, "").apply {
            path = "/"
            isHttpOnly = true
            domain = AppFacade.siteCookieDomain
            secure = true
            setAttribute("SameSite", "Strict")
            maxAge = 0
        })
    }
}
