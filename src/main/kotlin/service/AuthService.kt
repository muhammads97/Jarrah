package com.jarrah.service

import com.jarrah.domain.User
import com.jarrah.domain.UserEmail
import com.jarrah.domain.UserName
import com.jarrah.infra.UsersRepository
import com.jarrah.utilities.Database
import com.jarrah.utilities.PassowrdHasher
import com.jarrah.utilities.TokenUtils
import java.time.Instant
import java.util.UUID

object AuthService {
    private const val REFRESH_EXPIRY_SECONDS: Long = 60 * 60 * 24 * 30 // 30 days

    fun signup(name: String, email: String, password: String, deviceInfo: String?, deviceFingerprint: String?): TokenResult {
        return Database.withTransaction { conn ->
            val user = User(
                name = UserName(name),
                email = UserEmail(email)
            )
            UsersRepository.create(user, password, conn)
            val jwt = TokenUtils.generateJwt(user.id)
            val refreshTokenRaw = UsersRepository.createRefreshToken(
                conn,
                user.id,
                deviceInfo,
                deviceFingerprint,
                Instant.now().plusSeconds(REFRESH_EXPIRY_SECONDS)
            )

            TokenResult.Success(jwt, refreshTokenRaw)
        }
    }

    fun login(email: String, password: String, deviceInfo: String?, deviceFingerprint: String?): TokenResult {
        return Database.withTransaction { conn ->
            val record = UsersRepository.findByEmail(conn, email)
            val valid = UsersRepository.verifyPassword(password, record?.passwordHash?: "fakeHash") && record != null
            if (!valid) return@withTransaction TokenResult.InvalidToken

            if (!UsersRepository.verifyPassword(password, record.passwordHash)) {
                return@withTransaction TokenResult.InvalidToken
            }

            val jwt = TokenUtils.generateJwt(record.id)
            val refreshTokenRaw = UsersRepository.createRefreshToken(
                conn,
                record.id,
                deviceInfo,
                deviceFingerprint,
                Instant.now().plusSeconds(REFRESH_EXPIRY_SECONDS)
            )

            TokenResult.Success(jwt, refreshTokenRaw)
        }
    }

    fun rotateRefreshToken(oldTokenRaw: String, deviceInfo: String?, deviceFingerprint: String?): TokenResult {
        return Database.withTransaction { conn ->
            val parsed = TokenUtils.parseRefreshToken(oldTokenRaw)
                ?: return@withTransaction TokenResult.InvalidToken
            val (oldJti, oldTokenPart) = parsed
            val oldToken = UsersRepository.findRefreshTokenByJti(conn, oldJti)
                ?: return@withTransaction TokenResult.InvalidToken

            // Ensure it’s not expired or revoked
            if (oldToken.revoked) {
                return@withTransaction TokenResult.InvalidToken
            }

            val expectedHash = TokenUtils.hashTokenPart(oldTokenPart)
            if (!expectedHash.equals(oldToken.tokenHash, ignoreCase = true)) {
                return@withTransaction TokenResult.InvalidToken
            }

            if (oldToken.expiresAt.isBefore(Instant.now())) {
                // expire & revoke the row
                UsersRepository.revokeRefreshToken(conn, oldJti, replacedByJti = null)
                return@withTransaction TokenResult.InvalidToken
            }


            val newRefreshTokenRaw = UsersRepository.createRefreshToken(
                conn,
                oldToken.userId,
                deviceInfo ?: oldToken.deviceInfo,
                deviceFingerprint ?: oldToken.deviceFingerprint,
                Instant.now().plusSeconds(REFRESH_EXPIRY_SECONDS)
            )

            // Revoke old token and link to new
            UsersRepository.revokeRefreshToken(conn, oldJti, replacedByJti = newRefreshTokenRaw.substringBefore('.'))

            val newJwt = TokenUtils.generateJwt(oldToken.userId)

            TokenResult.Success(newJwt, newRefreshTokenRaw)
        }
    }

    fun forgotPassword(userEmail: String): ForgotPasswordResult {
        return Database.withTransaction { conn ->
            val user = UsersRepository.findByEmail(conn, userEmail)?: return@withTransaction ForgotPasswordResult.NotFound
            val resetToken = UUID.randomUUID().toString().replace("-", "")
            UsersRepository.insertPasswordResetToken(conn, resetToken, user.id)
            return@withTransaction ForgotPasswordResult.Success(resetToken)
        }
    }

    fun resetPassword(token: String, newPassword: String, deviceInfo: String?, deviceFingerprint: String?): TokenResult {
        return Database.withTransaction { conn ->
            val tokenRecord = UsersRepository.getPasswordResetTokenRecord(conn, token)
            if(tokenRecord == null || tokenRecord.expiresAt.isBefore(Instant.now())){
                return@withTransaction TokenResult.InvalidToken
            }
            UsersRepository.updatePassword(conn, newPassword, tokenRecord.userId)

            val jwt = TokenUtils.generateJwt(tokenRecord.id)
            val refreshTokenRaw = UsersRepository.createRefreshToken(
                conn,
                tokenRecord.userId,
                deviceInfo,
                deviceFingerprint,
                Instant.now().plusSeconds(REFRESH_EXPIRY_SECONDS)
            )
            TokenResult.Success(jwt, refreshTokenRaw)
        }
    }

    fun updatePassword(userId: UUID, oldPassword: String, newPassword: String): UpdatePasswordResult {
        return Database.withTransaction { conn ->
            val user = UsersRepository.findById(conn, userId) ?: return@withTransaction UpdatePasswordResult.NotFound
            if(!UsersRepository.verifyPassword(oldPassword, user.passwordHash)) return@withTransaction UpdatePasswordResult.IncorrectPassword
            UsersRepository.updatePassword(conn, newPassword, userId)
            return@withTransaction UpdatePasswordResult.Success
        }
    }

    // --------------------
    // DTOs
    // --------------------
    sealed interface TokenResult {
        data class Success(val jwt: String, val refreshToken: String): TokenResult
        object InvalidToken: TokenResult
    }

    sealed interface ForgotPasswordResult {
        data class Success(val resetToken: String) : ForgotPasswordResult
        data object NotFound: ForgotPasswordResult
    }

    sealed interface UpdatePasswordResult {
        data object Success : UpdatePasswordResult
        data object NotFound : UpdatePasswordResult
        data object IncorrectPassword : UpdatePasswordResult
    }
}