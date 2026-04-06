package com.unimarket.routes

import com.unimarket.models.*
import com.unimarket.services.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {

    route("/auth") {

        post("/register") {
            runCatching { call.receive<RegisterRequest>() }
                .onFailure {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                    return@post
                }
                .onSuccess { req ->
                    runCatching { AuthService.register(req) }
                        .onSuccess  { call.respond(HttpStatusCode.Created, it) }
                        .onFailure  {
                            when (it) {
                                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest,      ErrorResponse(it.message ?: "Bad request"))
                                is IllegalStateException    -> call.respond(HttpStatusCode.Conflict,        ErrorResponse(it.message ?: "Conflict"))
                                else                        -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error"))
                            }
                        }
                }
        }

        post("/login") {
            runCatching { call.receive<LoginRequest>() }
                .onFailure {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                    return@post
                }
                .onSuccess { req ->
                    runCatching { AuthService.login(req) }
                        .onSuccess  { call.respond(HttpStatusCode.OK, it) }
                        .onFailure  {
                            when (it) {
                                is NoSuchElementException -> call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
                                is IllegalStateException  -> call.respond(HttpStatusCode.Forbidden,   ErrorResponse(it.message ?: "Forbidden"))
                                else                      -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error"))
                            }
                        }
                }
        }
    }
}
