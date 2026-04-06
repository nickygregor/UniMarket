package com.unimarket.services

import com.unimarket.auth.JwtConfig
import com.unimarket.database.Users
import com.unimarket.database.dbQuery
import com.unimarket.models.*
import org.jetbrains.exposed.sql.*
import org.mindrot.jbcrypt.BCrypt

object AuthService {

    suspend fun register(req: RegisterRequest): AuthResponse {
        // UTA email check
        if (!req.email.lowercase().endsWith("@uta.edu"))
            throw IllegalArgumentException("Only UTA students can register. Email must end with @uta.edu")

        // Password length check
        if (req.password.length < 8)
            throw IllegalArgumentException("Password must be at least 8 characters")

        // Valid roles
        if (req.role !in listOf("BUYER", "SELLER", "BUYER_SELLER"))
            throw IllegalArgumentException("Invalid role selected")

        // Duplicate check
        val exists = dbQuery {
            Users.selectAll()
                .where { (Users.userId eq req.userId) or (Users.email eq req.email) }
                .count() > 0
        }
        if (exists) throw IllegalStateException("User ID or email already taken")

        val hash  = BCrypt.hashpw(req.password, BCrypt.gensalt())
        val newId = dbQuery {
            Users.insertAndGetId {
                it[firstName]    = req.firstName
                it[lastName]     = req.lastName
                it[email]        = req.email
                it[phoneNumber]  = req.phoneNumber
                it[userId]       = req.userId
                it[passwordHash] = hash
                it[role]         = req.role
                it[isActive]     = true
                it[createdAt]    = System.currentTimeMillis()
            }.value
        }

        val token = JwtConfig.generateToken(newId, req.userId, req.role)
        return AuthResponse(
            token = token,
            user  = UserResponse(newId, req.firstName, req.lastName,
                req.email, req.phoneNumber, req.userId, req.role, true)
        )
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        val row = dbQuery {
            Users.selectAll().where { Users.userId eq req.userId }.firstOrNull()
        } ?: throw NoSuchElementException("Invalid credentials")

        if (!row[Users.isActive])
            throw IllegalStateException("Account is deactivated. Contact admin.")
        if (!BCrypt.checkpw(req.password, row[Users.passwordHash]))
            throw NoSuchElementException("Invalid credentials")

        val dbId  = row[Users.id].value
        val token = JwtConfig.generateToken(dbId, req.userId, row[Users.role])
        return AuthResponse(
            token = token,
            user  = UserResponse(
                id          = dbId,
                firstName   = row[Users.firstName],
                lastName    = row[Users.lastName],
                email       = row[Users.email],
                phoneNumber = row[Users.phoneNumber],
                userId      = row[Users.userId],
                role        = row[Users.role],
                isActive    = row[Users.isActive]
            )
        )
    }
}
