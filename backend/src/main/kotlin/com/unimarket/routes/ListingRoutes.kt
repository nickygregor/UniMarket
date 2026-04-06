package com.unimarket.routes

import com.unimarket.models.*
import com.unimarket.services.ListingService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.listingRoutes() {

    // ── Public: browse & search ──────────────────────────────────────────────
    route("/listings") {

        get {
            val keyword  = call.request.queryParameters["keyword"]
            val category = call.request.queryParameters["category"]
            val listings = ListingService.getAll(keyword, category)
            call.respond(listings)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
            val listing = ListingService.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Listing not found"))
            call.respond(listing)
        }
    }

    // ── Seller: manage own listings ──────────────────────────────────────────
    authenticate("jwt") {

        route("/seller/listings") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("SELLER", "BUYER_SELLER")) return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Sellers only"))

                val sellerId = principal.payload.getClaim("dbId").asInt()
                call.respond(ListingService.getBySeller(sellerId))
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("SELLER", "BUYER_SELLER")) return@post call.respond(HttpStatusCode.Forbidden, ErrorResponse("Sellers only"))

                val sellerId = principal.payload.getClaim("dbId").asInt()
                val req      = call.receive<CreateListingRequest>()
                val created  = ListingService.create(sellerId, req)
                call.respond(HttpStatusCode.Created, created)
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("SELLER", "BUYER_SELLER")) return@put call.respond(HttpStatusCode.Forbidden, ErrorResponse("Sellers only"))

                val sellerId  = principal.payload.getClaim("dbId").asInt()
                val id        = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                val req       = call.receive<UpdateListingRequest>()

                runCatching { ListingService.update(id, sellerId, req) }
                    .onSuccess  { call.respond(it) }
                    .onFailure  {
                        when (it) {
                            is NoSuchElementException  -> call.respond(HttpStatusCode.NotFound,   ErrorResponse(it.message ?: "Not found"))
                            is IllegalAccessException  -> call.respond(HttpStatusCode.Forbidden,  ErrorResponse(it.message ?: "Forbidden"))
                            else                       -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error"))
                        }
                    }
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("SELLER", "BUYER_SELLER")) return@delete call.respond(HttpStatusCode.Forbidden, ErrorResponse("Sellers only"))

                val sellerId = principal.payload.getClaim("dbId").asInt()
                val id       = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))

                runCatching { ListingService.delete(id, sellerId) }
                    .onSuccess  { call.respond(MessageResponse("Listing deleted")) }
                    .onFailure  { call.respond(HttpStatusCode.Forbidden, ErrorResponse(it.message ?: "Error")) }
            }
        }
    }
}
