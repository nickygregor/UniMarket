package com.unimarket.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object DatabaseFactory {

    fun init() {
        Database.connect(
            url    = "jdbc:sqlite:unimarket.db",
            driver = "org.sqlite.JDBC"
        )
        transaction {
            SchemaUtils.create(Users, Listings, CartItems, Orders, OrderItems)
            seedAdmin()
        }
        println("✅ Database initialised — unimarket.db")
    }

    private fun seedAdmin() {
        val count = Users.selectAll().where { Users.role eq "ADMIN" }.count()
        if (count == 0L) {
            Users.insert {
                it[Users.firstName]    = "Admin"
                it[Users.lastName]     = "UniMarket"
                it[Users.email]        = "admin@unimarket.com"
                it[Users.phoneNumber]  = "0000000000"
                it[Users.userId]       = "admin"
                it[Users.passwordHash] = BCrypt.hashpw("admin123", BCrypt.gensalt())
                it[Users.role]         = "ADMIN"
                it[Users.isActive]     = true
                it[Users.createdAt]    = System.currentTimeMillis()
            }
            println("🔑 Default admin seeded → userId: admin / password: admin123")
        }
    }
}

fun <T> dbQuery(block: () -> T): T = transaction { block() }
