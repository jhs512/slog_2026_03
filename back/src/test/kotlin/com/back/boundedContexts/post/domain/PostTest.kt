package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostTest {

    private val author = Member(1, "user1", null, "작성자")

    @Test
    fun `softDelete 호출 시 deletedAt 이 설정된다`() {
        val post = Post(1, author, "제목", "내용", true, true)

        assertThat(post.deletedAt).isNull()
        post.softDelete()
        assertThat(post.deletedAt).isNotNull()
    }

    @Test
    fun `modify 호출 시 제목과 내용이 변경된다`() {
        val post = Post(1, author, "원래 제목", "원래 내용", true, true)

        post.modify("새 제목", "새 내용")

        assertThat(post.title).isEqualTo("새 제목")
        assertThat(post.content).isEqualTo("새 내용")
    }

    @Test
    fun `modify 시 published 를 false 로 변경하면 listed 도 false 가 된다`() {
        val post = Post(1, author, "제목", "내용", true, true)

        post.modify("제목", "내용", published = false)

        assertThat(post.published).isFalse()
        assertThat(post.listed).isFalse()
    }

    @Test
    fun `modify 시 published 를 지정하지 않으면 기존 값이 유지된다`() {
        val post = Post(1, author, "제목", "내용", true, true)

        post.modify("새 제목", "새 내용")

        assertThat(post.published).isTrue()
        assertThat(post.listed).isTrue()
    }

    @Test
    fun `modify 시 listed 를 개별 변경할 수 있다`() {
        val post = Post(1, author, "제목", "내용", true, true)

        post.modify("제목", "내용", listed = false)

        assertThat(post.published).isTrue()
        assertThat(post.listed).isFalse()
    }
}
