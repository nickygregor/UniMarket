package com.unimarket.routes

import com.unimarket.models.*
import com.unimarket.services.AdminService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes() {

    authenticate("jwt") {
        route("/admin") {

            fun isAdmin(principal: JWTPrincipal) =
                principal.payload.getClaim("role").asString() == "ADMIN"

            // List all users
            get("/users") {
                val principal = call.principal<JWTPrincipal>()!!
                if (!isAdmin(principal)) return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admins only"))
                call.respond(AdminService.getAllUsers())
            }

            // Activate / deactivate a user
            put("/users/{id}/activate") {
                val principal = call.principal<JWTPrincipal>()!!
                if (!isAdmin(principal)) return@put call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admins only"))
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                runCatching { AdminService.setUserActive(id, true) }
                    .onSuccess { call.respond(MessageResponse("User activated")) }
                    .onFailure { call.respond(HttpStatusCode.NotFound, ErrorResponse(it.message ?: "Not found")) }
            }

            put("/users/{id}/deactivate") {
                val principal = call.principal<JWTPrincipal>()!!
                if (!isAdmin(principal)) return@put call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admins only"))
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user id"))
                runCatching { AdminService.setUserActive(id, false) }
                    .onSuccess { call.respond(MessageResponse("User deactivated")) }
                    .onFailure { call.respond(HttpStatusCode.NotFound, ErrorResponse(it.message ?: "Not found")) }
            }

            // Remove a listing
            delete("/listings/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                if (!isAdmin(principal)) return@delete call.respond(HttpStatusCode.Forbidden, ErrorResponse("Admins only"))
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid listing id"))
                AdminService.removeListingAdmin(id)
                call.respond(MessageResponse("Listing removed"))
            }
        }
    }
}
