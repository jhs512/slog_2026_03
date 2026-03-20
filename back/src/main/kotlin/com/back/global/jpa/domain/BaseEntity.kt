package com.back.global.jpa.domain

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import org.hibernate.Hibernate

@MappedSuperclass
abstract class BaseEntity {
    abstract val id: Int

    @Transient
    private val attrCache: MutableMap<String, Any> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrPutAttr(key: String, defaultValue: () -> T): T =
        attrCache.getOrPut(key, defaultValue) as T

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is BaseEntity) return false
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        if (id == 0 || other.id == 0) return false

        return id == other.id
    }

    override fun hashCode(): Int =
        if (id != 0) 31 * Hibernate.getClass(this).hashCode() + id.hashCode()
        else super.hashCode()
}
