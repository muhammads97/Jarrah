package com.jarrah.domain

import java.util.UUID


data class User(
    val id: UUID = UUID.randomUUID(),
    val name: UserName,
    val email: UserEmail,
)

@JvmInline
value class UserName(val value: String){
    init {
        require(value.isNotBlank()) { "UserName cannot be blank" }
        require(value.length < 100) { "UserName cannot have more than 100 characters." }
    }
}

@JvmInline
value class UserEmail(val value: String) {
    init {
        require(value.isNotBlank()) { "UserEmail cannot be blank" }
        require(value.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {"Invalid email address"}
    }
}