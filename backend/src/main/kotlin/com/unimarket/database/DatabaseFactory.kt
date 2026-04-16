package com.unimarket.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
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
            migrateListingsImageUrlToTextIfNeeded()
            seedAdmin()
        }
        println("✅ Database initialised — unimarket.db")
    }

    private fun Transaction.migrateListingsImageUrlToTextIfNeeded() {
        val columnInfo = exec("PRAGMA table_info(listings)") { rs ->
            generateSequence {
                if (rs.next()) {
                    rs.getString("name") to rs.getString("type")
                } else {
                    null
                }
            }.toList()
        }.orEmpty()

        val imageColumnType = columnInfo.firstOrNull { it.first == "image_url" }?.second?.uppercase()
        if (imageColumnType == "TEXT") return

        exec(
            """
            CREATE TABLE IF NOT EXISTS listings_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                seller_id INTEGER NOT NULL,
                title VARCHAR(255) NOT NULL,
                description TEXT NOT NULL,
                price DOUBLE NOT NULL,
                category VARCHAR(100) NOT NULL,
                condition VARCHAR(50) NOT NULL DEFAULT 'Good',
                seller_contact VARCHAR(255) NOT NULL,
                image_url TEXT NULL,
                is_active BOOLEAN NOT NULL DEFAULT 1,
                expires_at BIGINT NOT NULL,
                created_at BIGINT NOT NULL,
                FOREIGN KEY(seller_id) REFERENCES users(id)
            )
            """.trimIndent()
        )
        exec(
            """
            INSERT INTO listings_new (
                id, seller_id, title, description, price, category, condition,
                seller_contact, image_url, is_active, expires_at, created_at
            )
            SELECT
                id, seller_id, title, description, price, category, condition,
                seller_contact, image_url, is_active, expires_at, created_at
            FROM listings
            """.trimIndent()
        )
        exec("DROP TABLE listings")
        exec("ALTER TABLE listings_new RENAME TO listings")
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
