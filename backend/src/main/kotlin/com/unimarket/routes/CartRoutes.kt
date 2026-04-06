package com.unimarket.routes

import com.unimarket.models.*
import com.unimarket.services.CartService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.cartRoutes() {

    authenticate("jwt") {
        route("/buyer/cart") {

            fun requireBuyer(principal: JWTPrincipal, onFail: suspend () -> Unit): Int? {
                val role = principal.payload.getClaim("role").asString()
                return if (role == "BUYER") principal.payload.getClaim("dbId").asInt() else null
            }

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("BUYER", "BUYER_SELLER")) return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Buyers only"))
                val buyerId   = principal.payload.getClaim("dbId").asInt()
                call.respond(CartService.getCart(buyerId))
            }

            post("/add") {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("BUYER", "BUYER_SELLER")) return@post call.respond(HttpStatusCode.Forbidden, ErrorResponse("Buyers only"))
                val buyerId   = principal.payload.getClaim("dbId").asInt()
                val req       = call.receive<AddToCartRequest>()
                runCatching { CartService.addItem(buyerId, req) }
                    .onSuccess { call.respond(MessageResponse("Item added to cart")) }
                    .onFailure { call.respond(HttpStatusCode.BadRequest, ErrorResponse(it.message ?: "Error")) }
            }

            delete("/remove/{cartItemId}") {
                val principal  = call.principal<JWTPrincipal>()!!
                val role       = principal.payload.getClaim("role").asString()
                if (role !in listOf("BUYER", "BUYER_SELLER")) return@delete call.respond(HttpStatusCode.Forbidden, ErrorResponse("Buyers only"))
                val buyerId    = principal.payload.getClaim("dbId").asInt()
                val cartItemId = call.parameters["cartItemId"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid cart item id"))
                CartService.removeItem(buyerId, cartItemId)
                call.respond(MessageResponse("Item removed"))
            }
        }
    }
}
