package com.jarrah.controller

import com.jarrah.service.AuthService
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

@Resource("/auth/signup")
class SignupResource()

@Resource("/auth/login")
class LoginResource()

@Resource("/auth/refresh")
class RefreshResource()

@Resource("/auth/forgot-password")
class ForgotPasswordResource()

@Resource("/auth/reset-password")
class ResetPasswordResource()

@Resource("/auth/me")
class MeResource()

@Serializable
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val deviceInfo: String? = null,
    val deviceFingerprint: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val deviceInfo: String? = null,
    val deviceFingerprint: String? = null
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceInfo: String? = null,
    val deviceFingerprint: String? = null
)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String,
    val deviceInfo: String? = null,
    val deviceFingerprint: String? = null
)

@Serializable
data class TokenResponse(val jwt: String, val refreshToken: String)

@Serializable
data class ForgotPasswordResponse(val token: String)

fun Route.authController() {
    // Signup
    post<SignupResource> {
        val request = call.receive<SignupRequest>()
            val result = AuthService.signup(
                request.name,
                request.email,
                request.password,
                request.deviceInfo,
                request.deviceFingerprint
            )
        when (result) {
            AuthService.TokenResult.InvalidToken -> call.respond(HttpStatusCode.Unauthorized)
            is AuthService.TokenResult.Success -> call.respond(HttpStatusCode.OK, result.toResponse())
        }
    }

    // Login
    post<LoginResource> {
        val request = call.receive<LoginRequest>()
        val result = AuthService.login(
                request.email,
                request.password,
                request.deviceInfo,
                request.deviceFingerprint
            )
        when (result) {
            AuthService.TokenResult.InvalidToken -> call.respond(HttpStatusCode.Unauthorized)
            is AuthService.TokenResult.Success -> call.respond(HttpStatusCode.OK, result.toResponse())
        }
    }

    // Refresh
    post<RefreshResource> {
        val request = call.receive<RefreshTokenRequest>()
        val result = AuthService.rotateRefreshToken(request.refreshToken, request.deviceInfo, request.deviceFingerprint)
        when (result) {
            AuthService.TokenResult.InvalidToken -> call.respond(HttpStatusCode.Unauthorized)
            is AuthService.TokenResult.Success -> call.respond(HttpStatusCode.OK, result.toResponse())
        }

    }

    // Forgot Password
    post<ForgotPasswordResource> {
        val request = call.receive<ForgotPasswordRequest>()
        val result = AuthService.forgotPassword(request.email)
        when(result){
            AuthService.ForgotPasswordResult.NotFound -> call.respond(HttpStatusCode.NotFound)
            is AuthService.ForgotPasswordResult.Success ->
                call.respond(HttpStatusCode.OK, ForgotPasswordResponse(result.resetToken))
        }
    }

    post<ResetPasswordResource> {
        val request = call.receive<ResetPasswordRequest>()
        val result = AuthService.resetPassword(
            request.token,
            request.newPassword,
            request.deviceInfo,
            request.deviceFingerprint
        )
        when(result){
            AuthService.TokenResult.InvalidToken -> call.respond(HttpStatusCode.Unauthorized)
            is AuthService.TokenResult.Success -> call.respond(HttpStatusCode.OK, result.toResponse())
        }

    }

    // Get current user (JWT auth required)
    authenticate("auth-jwt") {
        get<MeResource> {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("sub").asString()
            // Optionally, fetch full user from DB if needed
            call.respondText("Hello user $userId")
        }
    }

}

fun AuthService.TokenResult.Success.toResponse(): TokenResponse = TokenResponse(
    jwt,
    refreshToken
)
