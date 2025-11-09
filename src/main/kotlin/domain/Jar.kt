package com.jarrah.domain

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID
import java.time.Instant

const val JAR_MAX_AMOUNT = "9999999999.99"
data class Jar(
    val id: UUID,
    val ownerId: UUID,
    val title: JarTitle,
    val amount: JarAmount,
    val canSpend: Boolean,
    val canAdd: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun updateLocks(canAdd: Boolean, canSpend: Boolean): Jar = copy(canAdd = canAdd, canSpend = canSpend, updatedAt = Instant.now())
    companion object {
        fun create(ownerId: UUID, title: JarTitle): Pair<Jar, JarAccess> = UUID.randomUUID().let { jarId ->
            val jar = Jar(
                id = jarId,
                ownerId = ownerId,
                title = title,
                amount = JarAmount(BigDecimal.ZERO),
                canSpend = true,
                canAdd = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            val access = JarAccess(
                userId = ownerId,
                jarId = jarId,
                canAdd = true,
                canView = true,
                canSpend = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            Pair(jar, access)
        }
    }
}

data class JarAccess (
    val userId: UUID,
    val jarId: UUID,
    val canView: Boolean,
    val canAdd: Boolean,
    val canSpend: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

@JvmInline
@Serializable
value class JarTitle(val value: String){
    init {
        require(value.isNotBlank()) { "Jar title cannot be blank" }
        require(value.length < 20) { "Jar title cannot have more than 20 characters." }
    }
}

@JvmInline
@Serializable
value class JarAmount(@Contextual val value: BigDecimal){
    init {
        require(value >= BigDecimal.ZERO) { "Jar amount cannot have negative amount: $value." }
        require(value <= BigDecimal(JAR_MAX_AMOUNT)) { "Jar amount must be lower than $JAR_MAX_AMOUNT: $value" }
    }
}