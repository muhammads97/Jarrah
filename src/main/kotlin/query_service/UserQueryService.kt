package com.jarrah.query_service

import com.jarrah.domain.User
import com.jarrah.domain.UserEmail
import com.jarrah.domain.UserName
import com.jarrah.utilities.Database
import java.sql.ResultSet
import java.util.UUID

object UserQueryService {
    fun getUser(userId: UUID): User? {
        return Database.withTransaction { conn ->
            val stmt = conn.prepareStatement("""
                SELECT id, name, email FROM users WHERE id = ?;
            """.trimIndent())
            stmt.setObject(1, userId)
            val rs = stmt.executeQuery()
            if (rs.next()) return@withTransaction mapUserDomain(rs)
            return@withTransaction null
        }
    }

    fun mapUserDomain(rs: ResultSet): User = User(
        id = UUID.fromString(rs.getString("id")),
        name = UserName(rs.getString("name")),
        email = UserEmail(rs.getString("email")),
    )
}