package com.unimarket.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

object JwtConfig {
    private const val SECRET   = "unimarket-super-secret-key-cse3310-spring2026"
    private const val ISSUER   = "unimarket"
    private const val AUDIENCE = "unimarket-clients"
    private const val EXPIRY_MS = 86_400_000L   // 24 h

    const val REALM = "UniMarket Secure Zone"

    val algorithm: Algorithm = Algorithm.HMAC256(SECRET)

    fun verifier() = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()!!

    fun generateToken(dbId: Int, userId: String, role: String): String =
        JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("dbId",  dbId)
            .withClaim("userId", userId)
            .withClaim("role",  role)
            .withExpiresAt(Date(System.currentTimeMillis() + EXPIRY_MS))
            .sign(algorithm)

    fun getIssuer()   = ISSUER
    fun getAudience() = AUDIENCE
}
