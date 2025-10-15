package com.jarrah

import com.jarrah.controller.authController
import com.jarrah.controller.userController
import com.jarrah.infra.UsersRepository
import com.jarrah.utilities.JwtConfig
import com.jarrah.utilities.TokenUtils
import com.jarrah.utilities.UUIDSerializer
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.resources.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule


import org.flywaydb.core.Flyway
import java.util.UUID

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureMonitoring()
    install(Resources)
    install(ContentNegotiation) { json(
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
            serializersModule = SerializersModule {
                contextual(UUID::class, UUIDSerializer)
            }
        }
    ) }


    val config = environment.config.config("ktor.database")
    val url = config.property("url").getString()
    val user = config.property("user").getString()
    val password = config.property("password").getString()

    val flyway = Flyway.configure()
        .dataSource(url, user, password)
        .load()

    flyway.migrate()

    val secret = environment.config.propertyOrNull("auth.secret")?.getString()
        ?: "default-secret-for-dev-at-least-32-chars"
    val pepper = environment.config.propertyOrNull("auth.pepper")?.getString()
        ?: "default-pepper-for-dev"
    val issuer = environment.config.property("jwt.issuer").getString()
    val expiry = environment.config.property("jwt.expiry").getString().toLong()

    TokenUtils.init(JwtConfig(secret, issuer, expiry), pepper)
    UsersRepository.init(pepper)

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(TokenUtils.buildJwtVerifier())
            validate { credential ->
                if (credential.payload.getClaim("sub").asString() != null) JWTPrincipal(credential.payload) else null
            }
            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, null)
            }
        }
    }


    routing {
        authController()
        authenticate("auth-jwt") {
            userController()
        }
        get("/") {
            call.respondText("Hello World!")
        }
    }
}
