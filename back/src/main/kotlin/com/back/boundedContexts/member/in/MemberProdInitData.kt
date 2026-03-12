package com.back.boundedContexts.member.`in`

import com.back.boundedContexts.member.app.MemberFacade
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.transaction.annotation.Transactional

@Profile("prod")
@Configuration
class MemberProdInitData(
    private val memberFacade: MemberFacade,
    @Value("\${spring.datasource.password}")
    private val initialPassword: String
) {
    @Lazy
    @Autowired
    private lateinit var self: MemberProdInitData

    @Bean
    @Order(1)
    fun memberNotProdInitDataApplicationRunner(): ApplicationRunner {
        return ApplicationRunner {
            self.makeBaseMembers()
        }
    }

    @Transactional
    fun makeBaseMembers() {
        if (memberFacade.count() > 0) return

        val memberSystem = memberFacade.join("system", initialPassword, "시스템", null)
        memberSystem.modifyApiKey(memberSystem.username)

        val memberAdmin = memberFacade.join("admin", initialPassword, "관리자", null)
        memberAdmin.modifyApiKey(memberAdmin.username)

        val memberHolding = memberFacade.join("holding", initialPassword, "홀딩", null)
        memberHolding.modifyApiKey(memberHolding.username)
    }
}