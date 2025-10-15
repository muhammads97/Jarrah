package com.jarrah.controller

import com.jarrah.query_service.UserQueryService
import com.jarrah.service.AuthService
import com.jarrah.service.AuthService.UpdatePasswordResult
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable
import java.util.UUID

@Resource("/me")
class UserResource

@Resource("/update-password")
class UpdatePasswordResource

@Serializable
data class UpdatePasswordRequest(val oldPassword: String, val newPassword: String)

fun Route.userController() {
    get<UserResource>{
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("sub").asString()
        val user = UserQueryService.getUser(UUID.fromString(userId))
        if(user == null){
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(HttpStatusCode.OK, user)
        }
    }

    post<UpdatePasswordResource>{
        val principal = call.principal<JWTPrincipal>()
        val userId = principal!!.payload.getClaim("sub").asString()
        val request = call.receive<UpdatePasswordRequest>()
        val result = AuthService.updatePassword(UUID.fromString(userId), request.oldPassword, request.newPassword)
        when(result){
            is UpdatePasswordResult.IncorrectPassword -> call.respond(HttpStatusCode.Unauthorized)
            is UpdatePasswordResult.NotFound -> call.respond(HttpStatusCode.NotFound)
            is UpdatePasswordResult.Success -> call.respond(HttpStatusCode.OK)
        }
    }
}