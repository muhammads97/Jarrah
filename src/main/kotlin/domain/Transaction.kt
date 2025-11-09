package com.jarrah.domain

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

const val TRANSACTION_MAX = "9999999999.99"

data class Transaction(
    val id: UUID,
    val jarId: UUID,
    val userId: UUID,
    val amount: TransactionAmount,
    val title: TransactionTitle,
    val description: TransactionDescription,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun create(
            jar: Jar,
            userId: UUID,
            amount: TransactionAmount,
            title: TransactionTitle,
            description: TransactionDescription
        ): Pair<Jar, Transaction> {
            val id = UUID.randomUUID()
            val newJarAmount = JarAmount(jar.amount.value + amount.value)
            val updatedJar = jar.copy(amount = newJarAmount)
            val transaction = Transaction(
                id = id,
                userId = userId,
                jarId = jar.id,
                amount = amount,
                title = title,
                description = description,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            return Pair(updatedJar, transaction)
        }
    }
}

@JvmInline
@Serializable
value class TransactionTitle(val value: String?){
    init {
        require(value == null || value.length < 20) { "Transaction title cannot have more than 20 characters." }
    }
}

@JvmInline
@Serializable
value class TransactionDescription(val value: String?)

@JvmInline
@Serializable
value class TransactionAmount(@Contextual val value: BigDecimal){
    init {
        require(value != BigDecimal.ZERO) { "Transaction amount cannot have zero amount." }
        require(value <= BigDecimal(TRANSACTION_MAX)) { "Transaction must be lower than $TRANSACTION_MAX"}
        require(value > -BigDecimal(TRANSACTION_MAX)) { "Transaction must be greater than -$TRANSACTION_MAX"}
    }
}