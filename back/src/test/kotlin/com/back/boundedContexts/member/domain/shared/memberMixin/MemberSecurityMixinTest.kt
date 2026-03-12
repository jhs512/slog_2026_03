package com.back.boundedContexts.member.domain.shared.memberMixin

import com.back.IntegrationTest
import com.back.boundedContexts.member.app.MemberFacade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MemberSecurityMixinTest : IntegrationTest() {
    @Autowired
    private lateinit var memberFacade: MemberFacade

    @Test
    fun `관리자 회원은 ROLE_ADMIN 권한을 가진다`() {
        val admin = memberFacade.findByUsername("admin")!!

        assertThat(admin.isAdmin).isTrue()
        assertThat(admin.authoritiesAsStringList).containsExactly("ROLE_ADMIN")
        assertThat(admin.authorities.map { it.authority }).containsExactly("ROLE_ADMIN")
    }

    @Test
    fun `일반 회원은 관리자 권한을 가지지 않는다`() {
        val user1 = memberFacade.findByUsername("user1")!!

        assertThat(user1.isAdmin).isFalse()
        assertThat(user1.authoritiesAsStringList).isEmpty()
        assertThat(user1.authorities).isEmpty()
    }
}
