package com.back.boundedContexts.member.`in`.shared

import com.back.IntegrationMockMvcTest
import com.back.boundedContexts.member.app.MemberFacade
import com.back.boundedContexts.member.app.shared.AuthTokenService
import com.back.boundedContexts.member.app.shared.OneTimeTokenService
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler

class ApiV1AuthControllerTest : IntegrationMockMvcTest() {
    @Autowired
    private lateinit var memberFacade: MemberFacade

    @Autowired
    private lateinit var authTokenService: AuthTokenService

    @Autowired
    private lateinit var oneTimeTokenService: OneTimeTokenService

    @Nested
    inner class Login {
        @Test
        fun `로그인 요청이 성공하면 회원 정보와 토큰 그리고 쿠키를 반환한다`() {
            val resultActions = mvc.post("/member/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "username": "user1",
                        "password": "1234"
                    }
                    """.trimIndent()
            }

            val member = memberFacade.findByUsername("user1")!!

            resultActions.andExpect {
                status { isOk() }
                match(handler().handlerType(ApiV1AuthController::class.java))
                match(handler().methodName("login"))
                jsonPath("$.resultCode") { value("200-1") }
                jsonPath("$.msg") { value("${member.nickname}님 환영합니다.") }
                jsonPath("$.data.item.id") { value(member.id) }
                jsonPath("$.data.item.createdAt") { value(startsWith(member.createdAt.toString().take(20))) }
                jsonPath("$.data.item.modifiedAt") { value(startsWith(member.modifiedAt.toString().take(20))) }
                jsonPath("$.data.item.isAdmin") { value(member.isAdmin) }
                jsonPath("$.data.item.name") { value(member.name) }
                jsonPath("$.data.item.profileImageUrl") { value(member.redirectToProfileImgUrlOrDefault) }
                jsonPath("$.data.apiKey") { value(member.apiKey) }
                jsonPath("$.data.accessToken") { exists() }
            }

            val result = resultActions.andReturn()

            val apiKeyCookie = result.response.getCookie("apiKey")
            assertThat(apiKeyCookie).isNotNull
            assertThat(apiKeyCookie!!.value).isEqualTo(member.apiKey)
            assertThat(apiKeyCookie.path).isEqualTo("/")
            assertThat(apiKeyCookie.isHttpOnly).isTrue

            val accessTokenCookie = result.response.getCookie("accessToken")
            assertThat(accessTokenCookie).isNotNull
            assertThat(accessTokenCookie!!.value).isNotBlank()
            assertThat(accessTokenCookie.path).isEqualTo("/")
            assertThat(accessTokenCookie.isHttpOnly).isTrue
        }

        @Test
        fun `로그인 요청에서 비밀번호가 틀리면 401을 반환한다`() {
            mvc.post("/member/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "username": "user1",
                        "password": "wrong-password"
                    }
                    """.trimIndent()
            }.andExpect {
                status { isUnauthorized() }
                match(handler().handlerType(ApiV1AuthController::class.java))
                match(handler().methodName("login"))
                jsonPath("$.resultCode") { value("401-1") }
                jsonPath("$.msg") { value("비밀번호가 일치하지 않습니다.") }
            }
        }

        @Test
        fun `로그인 요청에서 존재하지 않는 username 을 보내면 401을 반환한다`() {
            mvc.post("/member/api/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                        "username": "nonexistent",
                        "password": "1234"
                    }
                    """.trimIndent()
            }.andExpect {
                status { isUnauthorized() }
                match(handler().handlerType(ApiV1AuthController::class.java))
                match(handler().methodName("login"))
                jsonPath("$.resultCode") { value("401-1") }
                jsonPath("$.msg") { value("존재하지 않는 아이디입니다.") }
            }
        }
    }

    @Nested
    inner class Logout {
        @Test
        fun `로그아웃 요청이 성공하면 인증 쿠키를 만료시킨다`() {
            val resultActions = mvc.delete("/member/api/v1/auth/logout")
                .andExpect {
                    status { isOk() }
                    match(handler().handlerType(ApiV1AuthController::class.java))
                    match(handler().methodName("logout"))
                    jsonPath("$.resultCode") { value("200-1") }
                    jsonPath("$.msg") { value("로그아웃 되었습니다.") }
                }

            val result = resultActions.andReturn()

            val apiKeyCookie: Cookie? = result.response.getCookie("apiKey")
            assertThat(apiKeyCookie).isNotNull
            assertThat(apiKeyCookie!!.value).isEmpty()
            assertThat(apiKeyCookie.maxAge).isEqualTo(0)
            assertThat(apiKeyCookie.path).isEqualTo("/")
            assertThat(apiKeyCookie.isHttpOnly).isTrue

            val accessTokenCookie: Cookie? = result.response.getCookie("accessToken")
            assertThat(accessTokenCookie).isNotNull
            assertThat(accessTokenCookie!!.value).isEmpty()
            assertThat(accessTokenCookie.maxAge).isEqualTo(0)
            assertThat(accessTokenCookie.path).isEqualTo("/")
            assertThat(accessTokenCookie.isHttpOnly).isTrue
        }
    }

    @Nested
    inner class OneTimeToken {
        @Test
        fun `인증된 사용자가 일회용 토큰을 발급받을 수 있다`() {
            val member = memberFacade.findByUsername("user1")!!

            mvc.post("/member/api/v1/auth/oneTimeToken") {
                cookie(Cookie("apiKey", member.apiKey))
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """{"allowedPathPrefix": "/sse/"}"""
            }.andExpect {
                status { isOk() }
                match(handler().handlerType(ApiV1AuthController::class.java))
                match(handler().methodName("oneTimeToken"))
                jsonPath("$.resultCode") { value("200-1") }
                jsonPath("$.data.oneTimeToken") { exists() }
            }
        }

        @Test
        fun `인증되지 않은 사용자는 일회용 토큰을 발급받을 수 없다`() {
            mvc.post("/member/api/v1/auth/oneTimeToken") {
                contentType = org.springframework.http.MediaType.APPLICATION_JSON
                content = """{"allowedPathPrefix": "/sse/"}"""
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `일반 API 경로에서는 일회용 토큰을 사용할 수 없다`() {
            val member = memberFacade.findByUsername("user1")!!
            val token = oneTimeTokenService.generate(member.id, "/member/api/v1/auth/")

            mvc.get("/member/api/v1/auth/me?oneTimeToken=$token")
                .andExpect {
                    status { isBadRequest() }
                    jsonPath("$.resultCode") { value("400-1") }
                }
        }

        @Test
        fun `일회용 토큰으로 허용되지 않은 경로 prefix에 접근할 수 없다`() {
            val member = memberFacade.findByUsername("user1")!!
            val token = oneTimeTokenService.generate(member.id, "/ws")

            mvc.get("/sse/test?oneTimeToken=$token")
                .andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.resultCode") { value("401-4") }
                }
        }

        @Test
        fun `유효하지 않은 일회용 토큰은 401을 반환한다`() {
            mvc.get("/sse/test?oneTimeToken=invalid-token")
                .andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.resultCode") { value("401-4") }
                }
        }
    }

    @Nested
    inner class Me {
        @Test
        fun `내 정보 조회는 apiKey 쿠키가 있으면 회원 정보를 반환한다`() {
            val member = memberFacade.findByUsername("user1")!!

            mvc.get("/member/api/v1/auth/me") {
                cookie(Cookie("apiKey", member.apiKey))
            }.andExpect {
                status { isOk() }
                match(handler().handlerType(ApiV1AuthController::class.java))
                match(handler().methodName("me"))
                jsonPath("$.id") { value(member.id) }
                jsonPath("$.createdAt") { value(startsWith(member.createdAt.toString().take(20))) }
                jsonPath("$.modifiedAt") { value(startsWith(member.modifiedAt.toString().take(20))) }
                jsonPath("$.isAdmin") { value(member.isAdmin) }
                jsonPath("$.username") { value(member.username) }
                jsonPath("$.name") { value(member.name) }
                jsonPath("$.nickname") { value(member.nickname) }
                jsonPath("$.profileImageUrl") { value(member.redirectToProfileImgUrlOrDefault) }
            }
        }

        @Test
        fun `내 정보 조회는 apiKey 쿠키가 없으면 401을 반환한다`() {
            mvc.get("/member/api/v1/auth/me")
                .andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.resultCode") { value("401-1") }
                    jsonPath("$.msg") { value("로그인 후 이용해주세요.") }
                }
        }

        @Test
        fun `내 정보 조회에서 Authorization 헤더가 Bearer 형식이 아니면 401을 반환한다`() {
            mvc.get("/member/api/v1/auth/me") {
                header(HttpHeaders.AUTHORIZATION, "key")
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.resultCode") { value("401-2") }
                jsonPath("$.msg") { value("Authorization 헤더가 Bearer 형식이 아닙니다.") }
            }
        }

        @Test
        fun `내 정보 조회에서 Authorization 헤더의 accessToken 이 잘못되어도 apiKey 가 유효하면 accessToken 을 재발급한다`() {
            val member = memberFacade.findByUsername("user1")!!

            val resultActions = mvc.get("/member/api/v1/auth/me") {
                header(HttpHeaders.AUTHORIZATION, "Bearer ${member.apiKey} wrong-access-token")
            }.andExpect {
                status { isOk() }
                match(handler().handlerType(ApiV1AuthController::class.java))
                match(handler().methodName("me"))
                jsonPath("$.id") { value(member.id) }
                jsonPath("$.createdAt") { value(startsWith(member.createdAt.toString().take(20))) }
                jsonPath("$.modifiedAt") { value(startsWith(member.modifiedAt.toString().take(20))) }
                jsonPath("$.isAdmin") { value(member.isAdmin) }
                jsonPath("$.username") { value(member.username) }
                jsonPath("$.name") { value(member.name) }
                jsonPath("$.nickname") { value(member.nickname) }
                jsonPath("$.profileImageUrl") { value(member.redirectToProfileImgUrlOrDefault) }
            }

            val result = resultActions.andReturn()
            val accessTokenCookie = result.response.getCookie("accessToken")

            assertThat(accessTokenCookie).isNotNull
            assertThat(accessTokenCookie!!.value).isNotBlank()
            assertThat(accessTokenCookie.path).isEqualTo("/")
            assertThat(accessTokenCookie.isHttpOnly).isTrue
            assertThat(result.response.getHeader(HttpHeaders.AUTHORIZATION))
                .isEqualTo(accessTokenCookie.value)
        }

        @Test
        fun `내 정보 조회에서 Authorization 헤더의 apiKey 와 accessToken 이 모두 유효하면 회원 정보를 반환하고 accessToken 을 재발급하지 않는다`() {
            val member = memberFacade.findByUsername("user1")!!
            val accessToken = authTokenService.genAccessToken(member)

            val resultActions = mvc.get("/member/api/v1/auth/me") {
                header(HttpHeaders.AUTHORIZATION, "Bearer ${member.apiKey} $accessToken")
            }.andExpect {
                status { isOk() }
                match(handler().handlerType(ApiV1AuthController::class.java))
                match(handler().methodName("me"))
                jsonPath("$.id") { value(member.id) }
                jsonPath("$.createdAt") { value(startsWith(member.createdAt.toString().take(20))) }
                jsonPath("$.modifiedAt") { value(startsWith(member.modifiedAt.toString().take(20))) }
                jsonPath("$.isAdmin") { value(member.isAdmin) }
                jsonPath("$.username") { value(member.username) }
                jsonPath("$.name") { value(member.name) }
                jsonPath("$.nickname") { value(member.nickname) }
                jsonPath("$.profileImageUrl") { value(member.redirectToProfileImgUrlOrDefault) }
            }

            val result = resultActions.andReturn()

            assertThat(result.response.getCookie("accessToken")).isNull()
            assertThat(result.response.getHeader(HttpHeaders.AUTHORIZATION)).isNull()
        }
    }
}
