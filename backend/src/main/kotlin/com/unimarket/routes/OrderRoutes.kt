package com.unimarket.routes

import com.unimarket.models.*
import com.unimarket.services.OrderService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes() {
    authenticate("jwt") {
        route("/buyer/orders") {

            // Checkout now requires card details in body
            post("/checkout") {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("BUYER", "BUYER_SELLER"))
                    return@post call.respond(HttpStatusCode.Forbidden, ErrorResponse("Buyers only"))
                val buyerId = principal.payload.getClaim("dbId").asInt()
                val req     = call.receive<CheckoutRequest>()
                runCatching { OrderService.checkout(buyerId, req) }
                    .onSuccess  { call.respond(HttpStatusCode.Created, it) }
                    .onFailure  {
                        when (it) {
                            is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest,   ErrorResponse(it.message ?: "Bad request"))
                            is IllegalStateException    -> call.respond(HttpStatusCode.BadRequest,   ErrorResponse(it.message ?: "Error"))
                            else                        -> call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Checkout failed"))
                        }
                    }
            }

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("BUYER", "BUYER_SELLER"))
                    return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Buyers only"))
                val buyerId = principal.payload.getClaim("dbId").asInt()
                call.respond(OrderService.getOrdersByBuyer(buyerId))
            }

            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val role      = principal.payload.getClaim("role").asString()
                if (role !in listOf("BUYER", "BUYER_SELLER"))
                    return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("Buyers only"))
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
                val order = OrderService.getOrder(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Order not found"))
                call.respond(order)
            }
        }
    }
}
