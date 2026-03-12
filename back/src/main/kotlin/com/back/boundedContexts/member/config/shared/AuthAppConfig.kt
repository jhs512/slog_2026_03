package com.back.boundedContexts.member.config.shared

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.out.shared.MemberAttrRepository
import org.springframework.context.annotation.Configuration

@Configuration
class AuthAppConfig(
    memberAttrRepository: MemberAttrRepository,
) {
    init {
        Member.attrRepository_ = memberAttrRepository
    }
}
