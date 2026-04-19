package com.unimarket

import com.unimarket.auth.JwtConfig
import com.unimarket.database.DatabaseFactory
import com.unimarket.models.ErrorResponse
import com.unimarket.routes.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    DatabaseFactory.init()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {

    // ── Content Negotiation (JSON) ───────────────────────────────────────────
    install(ContentNegotiation) {
        json(Json {
            prettyPrint        = true
            isLenient          = true
            ignoreUnknownKeys  = true
        })
    }

    // ── CORS (allow frontend + Android emulator) ─────────────────────────────
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()  // tighten in production
    }

    // ── Default Headers ──────────────────────────────────────────────────────
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
    }

    // ── JWT Authentication ───────────────────────────────────────────────────
    install(Authentication) {
        jwt("jwt") {
            realm     = JwtConfig.REALM
            verifier(JwtConfig.verifier())
            validate { credential ->
                if (credential.payload.getClaim("userId").asString().isNotEmpty())
                    JWTPrincipal(credential.payload)
                else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Token is invalid or expired"))
            }
        }
    }

    // ── Status Pages (global error handling) ─────────────────────────────────
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.localizedMessage ?: "Unknown error"))
        }
    }

    // ── Routing ──────────────────────────────────────────────────────────────
    routing {
        get("/") { call.respond(mapOf("status" to "UniMarket API running 🚀", "version" to "1.0.0")) }
        authRoutes()
        listingRoutes()
        cartRoutes()
        orderRoutes()
        adminRoutes()
        interactionRoutes()
    }
}
