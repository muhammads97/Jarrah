package com.jarrah.infra

import com.jarrah.domain.Jar
import com.jarrah.domain.JarAccess
import com.jarrah.domain.JarAmount
import com.jarrah.domain.JarTitle
import java.sql.Connection
import java.time.Instant
import java.util.UUID

object JarsRepository {

    fun create(jar: Jar, conn: Connection) {
        // lang=sql
        val sql = """
            INSERT INTO jars (
                id, title, owner_id, amount, can_spend, can_add, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, jar.id)
            stmt.setObject(2, jar.title)
            stmt.setObject(3, jar.ownerId)
            stmt.setBigDecimal(4, jar.amount.value)
            stmt.setBoolean(5, jar.canSpend)
            stmt.setBoolean(6, jar.canAdd)
            stmt.setObject(7, jar.createdAt)
            stmt.setObject(8, jar.updatedAt)
            stmt.executeUpdate()
        }
    }

    fun getById(jarId: UUID, conn: Connection): Jar? {
        val sql = """
            SELECT id, title, owner_id, amount, can_spend, can_add, updated_at, created_at from jars where id = ? and deleted_at is null;
        """.trimIndent()
        return conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, jarId)
            return stmt.executeQuery().use { rs ->
                return if (rs.next()) {
                    Jar(
                        id = UUID.fromString(rs.getString("id")),
                        ownerId = UUID.fromString(rs.getString("owner_id")),
                        title = JarTitle(rs.getString("title")),
                        amount = JarAmount(rs.getBigDecimal("amount")),
                        canSpend = rs.getBoolean("can_spend"),
                        canAdd = rs.getBoolean("can_add"),
                        createdAt = rs.getTimestamp("created_at").toInstant(),
                        updatedAt = rs.getTimestamp("updated_at").toInstant(),
                    )
                } else null
            }
        }
    }

    fun delete(jar: Jar, conn: Connection) {
        val sql = """
            UPDATE jars SET deleted_at = ?, updated_at = ? where id = ?;
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, Instant.now())
            stmt.setObject(2, Instant.now())
            stmt.setObject(3, jar.id)
            stmt.executeUpdate()
        }
    }

    fun update(jar: Jar, conn: Connection) {
        val sql = """
            UPDATE jars SET title = ?, amount = ?, can_spend = ?, can_add = ?, updated_at = ? where id = ?;
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, jar.title)
            stmt.setBigDecimal(2, jar.amount.value)
            stmt.setBoolean(3, jar.canSpend)
            stmt.setBoolean(4, jar.canAdd)
            stmt.setObject(5, Instant.now())
            stmt.setObject(6, jar.id)
            stmt.executeUpdate()
        }
    }

    fun createOrUpdateAccess(jarAccess: JarAccess, conn: Connection) {
        // language=sql
        val sql = """
        INSERT INTO jar_accesses (
            jar_id, user_id, can_view, can_add, can_spend, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (jar_id, user_id)
        DO UPDATE SET
            can_view = EXCLUDED.can_view,
            can_add = EXCLUDED.can_add,
            can_spend = EXCLUDED.can_spend,
            updated_at = EXCLUDED.updated_at;
    """.trimIndent()

        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, jarAccess.jarId)
            stmt.setObject(2, jarAccess.userId)
            stmt.setBoolean(3, jarAccess.canView)
            stmt.setBoolean(4, jarAccess.canAdd)
            stmt.setBoolean(5, jarAccess.canSpend)
            stmt.setObject(6, jarAccess.createdAt)
            stmt.setObject(7, jarAccess.updatedAt)
            stmt.executeUpdate()
        }
    }
}