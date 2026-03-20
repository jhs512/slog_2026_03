package com.back.boundedContexts.member.domain.shared

import com.back.boundedContexts.post.domain.Post
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class MemberTest {
    @Test
    fun `SYSTEM 회원은 system 고정 정보로 생성된다`() {
        assertThat(MemberPolicy.SYSTEM.id).isEqualTo(1)
        assertThat(MemberPolicy.SYSTEM.username).isEqualTo("system")
        assertThat(MemberPolicy.SYSTEM.nickname).isEqualTo("시스템")
        assertThat(MemberPolicy.SYSTEM.name).isEqualTo("시스템")
        assertThat(MemberPolicy.SYSTEM.isAdmin).isTrue()
    }

    @Test
    fun `genApiKey 는 UUID 형식의 문자열을 생성한다`() {
        val apiKey1 = MemberPolicy.genApiKey()
        val apiKey2 = MemberPolicy.genApiKey()

        assertThat(apiKey1).matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        assertThat(apiKey2).matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        assertThat(apiKey1).isNotEqualTo(apiKey2)
    }

    @Test
    fun `저장 전 엔티티는 id가 0이어도 서로 같지 않다`() {
        val member1 = Member(0, "user-a", null, "유저A")
        val member2 = Member(0, "user-b", null, "유저B")

        assertThat(member1).isNotEqualTo(member2)
    }

    @Test
    fun `다른 엔티티 타입은 같은 id여도 같지 않다`() {
        val member = Member(1, "user1", null, "유저1")
        val post = Post(1, member, "제목", "내용", true, true)

        assertThat(member).isNotEqualTo(post)
    }

    @Test
    fun `MemberProxy 는 real 을 한 번 사용한 뒤부터 nickname 과 name 도 real 기준으로 본다`() {
        val real = Member(1, "user1", null, "DB닉네임")
        real.createdAt = Instant.now()
        real.modifiedAt = Instant.now()

        val proxy = MemberProxy(real, 1, "user1", "토큰닉네임")

        assertThat(proxy.nickname).isEqualTo("토큰닉네임")
        assertThat(proxy.name).isEqualTo("토큰닉네임")

        proxy.createdAt

        assertThat(proxy.nickname).isEqualTo("DB닉네임")
        assertThat(proxy.name).isEqualTo("DB닉네임")
    }
}
