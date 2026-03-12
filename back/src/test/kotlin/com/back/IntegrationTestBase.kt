package com.back

import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@Transactional
@ActiveProfiles("test")
abstract class IntegrationTestBase
