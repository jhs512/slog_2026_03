package com.back.boundedContexts.member.out.shared

import com.back.boundedContexts.member.domain.shared.MemberAttr
import org.springframework.data.jpa.repository.JpaRepository

interface MemberAttrRepository : JpaRepository<MemberAttr, Int>, MemberAttrRepositoryCustom
