package com.unimarket.services

import com.unimarket.auth.JwtConfig
import com.unimarket.database.Users
import com.unimarket.database.dbQuery
import com.unimarket.models.*
import org.jetbrains.exposed.sql.*
import org.mindrot.jbcrypt.BCrypt

object AuthService {

    suspend fun register(req: RegisterRequest): AuthResponse {
        if (!req.email.lowercase().endsWith("@mavs.uta.edu")) {
            throw IllegalArgumentException("Use your UTA student email (@mavs.uta.edu)")
        }

        if (req.password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters")
        }

        val exists = dbQuery {
            Users.selectAll()
                .where { (Users.userId eq req.userId) or (Users.email eq req.email) }
                .count() > 0
        }
        if (exists) throw IllegalStateException("User ID or email already taken")

        val hash = BCrypt.hashpw(req.password, BCrypt.gensalt())
        val defaultRole = "BUYER_SELLER"

        val newId = dbQuery {
            Users.insertAndGetId {
                it[firstName] = req.firstName
                it[lastName] = req.lastName
                it[email] = req.email
                it[phoneNumber] = req.phoneNumber
                it[userId] = req.userId
                it[passwordHash] = hash
                it[role] = defaultRole
                it[isActive] = true
                it[createdAt] = System.currentTimeMillis()
            }.value
        }

        val token = JwtConfig.generateToken(newId, req.userId, defaultRole)

        return AuthResponse(
            token = token,
            user = UserResponse(
                id = newId,
                firstName = req.firstName,
                lastName = req.lastName,
                email = req.email,
                phoneNumber = req.phoneNumber,
                userId = req.userId,
                role = defaultRole,
                isActive = true
            )
        )
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        val row = dbQuery {
            Users.selectAll().where { Users.userId eq req.userId }.firstOrNull()
        } ?: throw NoSuchElementException("Invalid credentials")

        if (!row[Users.isActive]) {
            throw IllegalStateException("Account is deactivated")
        }

        if (!BCrypt.checkpw(req.password, row[Users.passwordHash])) {
            throw NoSuchElementException("Invalid credentials")
        }

        val dbId = row[Users.id].value
        val role = row[Users.role]

        val token = JwtConfig.generateToken(dbId, req.userId, role)

        return AuthResponse(
            token = token,
            user = UserResponse(
                id = dbId,
                firstName = row[Users.firstName],
                lastName = row[Users.lastName],
                email = row[Users.email],
                phoneNumber = row[Users.phoneNumber],
                userId = row[Users.userId],
                role = role,
                isActive = row[Users.isActive]
            )
        )
    }

    suspend fun changePassword(userDbId: Int, req: ChangePasswordRequest): MessageResponse {
        if (req.newPassword.length < 8) {
            throw IllegalArgumentException("New password must be at least 8 characters")
        }

        val row = dbQuery {
            Users.selectAll().where { Users.id eq userDbId }.firstOrNull()
        } ?: throw NoSuchElementException("User not found")

        if (!BCrypt.checkpw(req.currentPassword, row[Users.passwordHash])) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        val newHash = BCrypt.hashpw(req.newPassword, BCrypt.gensalt())
        dbQuery {
            Users.update({ Users.id eq userDbId }) {
                it[passwordHash] = newHash
            }
        }

        return MessageResponse("Password changed successfully")
    }
}
