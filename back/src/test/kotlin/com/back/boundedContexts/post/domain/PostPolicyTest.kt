package com.back.boundedContexts.post.domain

import com.back.boundedContexts.member.domain.shared.Member
import com.back.global.exception.app.AppException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PostPolicyTest {

    private val author = Member(1, "user1", null, "작성자")
    private val admin = Member(2, "admin", null, "관리자")
    private val other = Member(3, "other", null, "다른사용자")

    @Nested
    inner class CanRead {
        @Test
        fun `공개 글은 누구나 조회할 수 있다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            assertThat(post.canRead(null)).isTrue()
            assertThat(post.canRead(other)).isTrue()
        }

        @Test
        fun `미공개 글은 작성자만 조회할 수 있다`() {
            val post = Post(1, author, "제목", "내용", false, false)

            assertThat(post.canRead(author)).isTrue()
            assertThat(post.canRead(other)).isFalse()
            assertThat(post.canRead(null)).isFalse()
        }

        @Test
        fun `미공개 글은 관리자도 조회할 수 있다`() {
            val post = Post(1, author, "제목", "내용", false, false)

            assertThat(post.canRead(admin)).isTrue()
        }

        @Test
        fun `미공개 글을 권한 없는 사용자가 조회하면 예외가 발생한다`() {
            val post = Post(1, author, "제목", "내용", false, false)

            assertThatThrownBy { post.checkActorCanRead(other) }
                .isInstanceOf(AppException::class.java)
        }
    }

    @Nested
    inner class CanModify {
        @Test
        fun `작성자는 글을 수정할 수 있다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            val rs = post.getCheckActorCanModifyRs(author)

            assertThat(rs.isSuccess).isTrue()
        }

        @Test
        fun `다른 사용자는 글을 수정할 수 없다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            val rs = post.getCheckActorCanModifyRs(other)

            assertThat(rs.isFail).isTrue()
            assertThat(rs.resultCode).isEqualTo("403-1")
        }

        @Test
        fun `비로그인 사용자는 글을 수정할 수 없다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            val rs = post.getCheckActorCanModifyRs(null)

            assertThat(rs.isFail).isTrue()
            assertThat(rs.resultCode).isEqualTo("401-1")
        }

        @Test
        fun `checkActorCanModify 는 권한 없을 때 예외를 던진다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            assertThatThrownBy { post.checkActorCanModify(other) }
                .isInstanceOf(AppException::class.java)
        }
    }

    @Nested
    inner class CanDelete {
        @Test
        fun `작성자는 글을 삭제할 수 있다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            val rs = post.getCheckActorCanDeleteRs(author)

            assertThat(rs.isSuccess).isTrue()
        }

        @Test
        fun `관리자는 다른 사람 글을 삭제할 수 있다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            val rs = post.getCheckActorCanDeleteRs(admin)

            assertThat(rs.isSuccess).isTrue()
        }

        @Test
        fun `다른 사용자는 글을 삭제할 수 없다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            val rs = post.getCheckActorCanDeleteRs(other)

            assertThat(rs.isFail).isTrue()
            assertThat(rs.resultCode).isEqualTo("403-2")
        }

        @Test
        fun `비로그인 사용자는 글을 삭제할 수 없다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            val rs = post.getCheckActorCanDeleteRs(null)

            assertThat(rs.isFail).isTrue()
            assertThat(rs.resultCode).isEqualTo("401-1")
        }

        @Test
        fun `checkActorCanDelete 는 권한 없을 때 예외를 던진다`() {
            val post = Post(1, author, "제목", "내용", true, true)

            assertThatThrownBy { post.checkActorCanDelete(other) }
                .isInstanceOf(AppException::class.java)
        }
    }
}
