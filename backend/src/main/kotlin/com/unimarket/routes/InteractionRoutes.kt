package com.unimarket.routes

import com.unimarket.models.CreateCommentRequest
import com.unimarket.models.ErrorResponse
import com.unimarket.models.SendMessageRequest
import com.unimarket.services.InteractionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.interactionRoutes() {
    route("/listings/{listingId}/comments") {
        get {
            val listingId = call.parameters["listingId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid listing id"))
            call.respond(InteractionService.getComments(listingId))
        }
    }

    authenticate("jwt") {
        route("/listings/{listingId}/comments") {
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("dbId").asInt()
                val listingId = call.parameters["listingId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid listing id"))
                val req = call.receive<CreateCommentRequest>()

                runCatching { InteractionService.addComment(listingId, userId, req) }
                    .onSuccess { call.respond(HttpStatusCode.Created, it) }
                    .onFailure {
                        when (it) {
                            is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Bad request"))
                            is NoSuchElementException -> call.respond(HttpStatusCode.NotFound, ErrorResponse(it.message ?: "Not found"))
                            else -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Could not add comment"))
                        }
                    }
            }
        }

        route("/seller/interactions") {
            get("/comments") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role !in listOf("SELLER", "BUYER_SELLER")) {
                    return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Sellers only"))
                }
                val sellerId = principal.payload.getClaim("dbId").asInt()
                call.respond(InteractionService.sellerComments(sellerId))
            }

            get("/notifications") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role !in listOf("SELLER", "BUYER_SELLER")) {
                    return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Sellers only"))
                }
                val sellerId = principal.payload.getClaim("dbId").asInt()
                call.respond(InteractionService.sellerNotifications(sellerId))
            }
        }

        route("/messages") {
            get("/conversations") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("dbId").asInt()
                call.respond(InteractionService.conversations(userId))
            }

            get("/listings/{listingId}/users/{otherUserId}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("dbId").asInt()
                val listingId = call.parameters["listingId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid listing id"))
                val otherUserId = call.parameters["otherUserId"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                call.respond(InteractionService.getThread(listingId, userId, otherUserId))
            }

            post("/listings/{listingId}/users/{otherUserId}") {
                val principal = call.principal<JWTPrincipal>()!!
                val userId = principal.payload.getClaim("dbId").asInt()
                val listingId = call.parameters["listingId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid listing id"))
                val otherUserId = call.parameters["otherUserId"]?.toIntOrNull()
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                val req = call.receive<SendMessageRequest>()

                runCatching { InteractionService.sendMessage(listingId, userId, otherUserId, req.message) }
                    .onSuccess { call.respond(HttpStatusCode.Created, it) }
                    .onFailure {
                        when (it) {
                            is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Bad request"))
                            is IllegalAccessException -> call.respond(HttpStatusCode.Forbidden, ErrorResponse(it.message ?: "Forbidden"))
                            is NoSuchElementException -> call.respond(HttpStatusCode.NotFound, ErrorResponse(it.message ?: "Not found"))
                            else -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Could not send message"))
                        }
                    }
            }
        }
    }
}
