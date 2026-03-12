package com.back.standard.dto

import java.util.*

interface Payload {
    val uid: UUID
    val aggregateType: String
    val aggregateId: Int
}
