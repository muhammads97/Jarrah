package com.jarrah.utilities

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.jsonwebtoken.JwtBuilder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import javax.crypto.SecretKey


data class JwtConfig(
    val secret: String,
    val issuer: String,
    val expiry: Long = 3600,
) {
    init {
        require(secret.length >= 32) { "JWT secret must be at least 32 characters" }
    }
}

object TokenUtils {
    private lateinit var jwtConfig: JwtConfig
    private lateinit var pepper: String
    private val secureRandom = SecureRandom()

    fun init(config: JwtConfig, pepperString: String) {
        jwtConfig = config
        pepper = pepperString
    }

    fun generateRefreshTokenRaw(): Pair<String, String> {
        val jti = UUID.randomUUID().toString()
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        val tokenPart = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val raw = "$jti.$tokenPart"
        return Pair(raw, tokenPart)
    }

    fun hashTokenPart(tokenPart: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((tokenPart + pepper).toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun parseRefreshToken(raw: String): Pair<String, String>? {
        val idx = raw.indexOf('.')
        if(idx <= 0) return null
        val jti = raw.substring(0, idx)
        val tokenPart = raw.substring(idx + 1)
        return Pair(jti, tokenPart)
    }

    fun generateJwt(userId: UUID): String {
        val now = Instant.now()
        val expiry = now.plusSeconds(jwtConfig.expiry)

        // Create HMAC key from secret
        val key: SecretKey = Keys.hmacShaKeyFor(jwtConfig.secret.toByteArray())

        // Use builder with claims directly via modern api
        val builder: JwtBuilder = Jwts.builder()
            .claim("sub", userId.toString())
            .claim("iss", jwtConfig.issuer)
            .claim("iat", now.epochSecond)
            .claim("exp", expiry.epochSecond)
            .signWith(key) // modern signWith, no deprecated enums

        return builder.compact()
    }

    fun buildJwtVerifier(): JWTVerifier {
        val key: SecretKey = Keys.hmacShaKeyFor(jwtConfig.secret.toByteArray())
        return JWT
            .require(Algorithm.HMAC256(key.encoded))
            .withIssuer(jwtConfig.issuer)
            .build()
    }
}