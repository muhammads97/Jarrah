package com.jarrah.utilities

import java.sql.Connection
import java.sql.DriverManager

object Database {
    private val url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/jarrah_local"
    private val user = System.getenv("DATABASE_USER") ?: "jarrah_db_user"
    private val password = System.getenv("DATABASE_PASSWORD") ?: "123123123"

    init {
        Class.forName("org.postgresql.Driver")
    }

    fun getConnection(): Connection {
        return DriverManager.getConnection(url, user, password)
    }

    fun <T> withTransaction(block: (Connection) -> T): T {
        getConnection().use { conn ->
            try {
                conn.autoCommit = false
                val result = block(conn)
                conn.commit()
                return result
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }
}