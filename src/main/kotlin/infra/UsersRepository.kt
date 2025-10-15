package com.jarrah.infra

import com.jarrah.domain.User
import com.jarrah.utilities.TokenUtils
import io.ktor.utils.io.InternalAPI
import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class UserRecord(
    val id: UUID,
    val name: String,
    val email: String,
    val passwordHash: String,
    val tokenVersion: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class RefreshTokenRecord(
    val id: UUID,
    val userId: UUID,
    val jti: String,
    val tokenHash: String,
    val deviceInfo: String?,
    val deviceFingerprint: String?,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val revoked: Boolean,
    val replacedByJti: String?,
    val lastUsedAt: Instant?
)

data class PasswordResetTokenRecord(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val used: Boolean
)

object UsersRepository {
    private lateinit var pepper: String

    fun init(pepperValue: String) {
        pepper = pepperValue
    }
    fun create(user: User, password: String, conn: Connection) {
        // lang = sql
        val sql = """
            INSERT INTO users (
                id, 
                name,
                email,
                password_hash,
                token_version,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?);
        """.trimIndent()
        conn.prepareStatement(sql).use { statement ->
            statement.setObject(1, user.id)
            statement.setString(2, user.name.value)
            statement.setString(3, user.email.value)
            statement.setString(4, hashPassword(password))
            statement.setInt(5, 1)
            statement.setObject(6, Timestamp.from(Instant.now()))
            statement.setObject(7, Timestamp.from(Instant.now()))
            statement.executeUpdate()
        }
    }

    fun updatePassword(conn: Connection, newPassword: String, userId: UUID) {
        conn.prepareStatement("""
            UPDATE users SET password_hash = ? WHERE id = ?;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, hashPassword(newPassword))
            stmt.setObject(2, userId)
            stmt.executeUpdate()
        }
    }

    fun findByEmail(conn: Connection, email: String): UserRecord? {
        val sql = "SELECT id, name, email, password_hash, token_version, created_at, updated_at FROM users WHERE email = ?"
        return conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, email)
            stmt.executeQuery().use { rs ->
                if (rs.next()) mapUserRow(rs) else null
            }
        }
    }

    fun verifyPassword(plain: String, hash: String): Boolean =
        BCrypt.checkpw(plain + pepper, hash)

    fun createRefreshToken(conn: Connection, userId: UUID, deviceInfo: String?, deviceFingerprint: String?, expiresAt: Instant): String {
        val (raw, tokenPart) = TokenUtils.generateRefreshTokenRaw()
        val jti = raw.substringBefore('.')
        val tokenHash = TokenUtils.hashTokenPart(tokenPart)
        conn.prepareStatement(
            """
                INSERT INTO refresh_tokens (
                    id,
                    user_id,
                    jti,
                    token_hash,
                    device_info,
                    device_fingerprint,
                    issued_at,
                    expires_at,
                    revoked
                ) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?, false);
            """.trimIndent()
        ).use {stmt ->
            stmt.setObject(1, UUID.randomUUID())
            stmt.setObject(2, userId)
            stmt.setString(3, jti)
            stmt.setString(4, tokenHash)
            stmt.setString(5, deviceInfo)
            stmt.setString(6, deviceFingerprint)
            stmt.setObject(7, Timestamp.from(expiresAt))
            stmt.executeUpdate()
        }
        return raw
    }

    fun findRefreshTokenByJti(conn: Connection, jti: String): RefreshTokenRecord? {
        return conn.prepareStatement(
            """
                SELECT * from refresh_tokens WHERE jti = ?;
            """.trimIndent()
        ).use {stmt ->
            stmt.setString(1, jti)
            stmt.executeQuery().use { rs -> if (rs.next()) mapRefreshTokenRow(rs) else null }
        }
    }

    fun revokeRefreshToken(conn: Connection, jti: String, replacedByJti: String? = null) {
        conn.prepareStatement(
            """
            UPDATE refresh_tokens
            SET revoked = true, replaced_by_jti = ?
            WHERE jti = ?
            """
        ).use { stmt ->
            stmt.setString(1, replacedByJti)
            stmt.setString(2, jti)
            stmt.executeUpdate()
        }
    }

    fun insertPasswordResetToken(conn: Connection, token: String, userId: UUID) {
        conn.prepareStatement("""
            INSERT INTO password_reset_tokens (
                id, 
                user_id, 
                token_hash, 
                expires_at, 
                created_at, 
                used
            ) VALUES (gen_random_uuid(), ?, ?, ?, NOW(), false);
        """.trimIndent()).use { stmt ->
            stmt.setObject(1, userId)
            stmt.setString(2, token)
            stmt.setObject(3, Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            stmt.executeUpdate()
        }
    }

    fun getPasswordResetTokenRecord(conn: Connection, token: String): PasswordResetTokenRecord? {
        return conn.prepareStatement("""
            SELECT * from password_reset_tokens WHERE token_hash = ?;
        """.trimIndent()).use { stmt ->
            stmt.setString(1, token)
            stmt.executeQuery().use { rs ->
                if (rs.next()) mapPasswordResetTokenRecord(rs)
                else null
            }
        }
    }

    @OptIn(InternalAPI::class)
    private fun mapUserRow(rs: ResultSet): UserRecord =
        UserRecord(
            id = rs.getObject("id", UUID::class.java),
            name = rs.getString("name"),
            email = rs.getString("email"),
            passwordHash = rs.getString("password_hash"),
            tokenVersion = rs.getInt("token_version"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            updatedAt = rs.getTimestamp("updated_at").toInstant()
        )

    private fun mapRefreshTokenRow(rs: ResultSet): RefreshTokenRecord =
        RefreshTokenRecord(
            id = rs.getObject("id", UUID::class.java),
            userId = rs.getObject("user_id", UUID::class.java),
            jti = rs.getString("jti"),
            tokenHash = rs.getString("token_hash"),
            deviceInfo = rs.getString("device_info"),
            deviceFingerprint = rs.getString("device_fingerprint"),
            issuedAt = rs.getTimestamp("issued_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
            revoked = rs.getBoolean("revoked"),
            replacedByJti = rs.getString("replaced_by_jti"),
            lastUsedAt = rs.getTimestamp("last_used_at")?.toInstant()
        )
    private fun mapPasswordResetTokenRecord(rs: ResultSet): PasswordResetTokenRecord = PasswordResetTokenRecord(
        id = rs.getObject("id", UUID::class.java),
        userId = rs.getObject("user_id", UUID::class.java),
        tokenHash = rs.getString("token_hash"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        expiresAt = rs.getTimestamp("expires_at").toInstant(),
        used = rs.getBoolean("used")
    )
    private fun hashPassword(plain: String): String =
        BCrypt.hashpw(plain + pepper, BCrypt.gensalt())
}