package com.unimarket.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ── Entity ────────────────────────────────────────────────────────────────────

@Entity(tableName = "cached_listings")
data class ListingEntity(
    @PrimaryKey val id          : Int,
    val sellerId    : Int,
    val sellerName  : String,
    val title       : String,
    val description : String,
    val price       : Double,
    val category    : String,
    val condition   : String = "Good",
    val sellerContact : String = "",
    val imageUrl    : String?,
    val isActive    : Boolean,
    val expiresAt   : Long = 0L,
    val createdAt   : Long
)

// ── DAO ───────────────────────────────────────────────────────────────────────

@Dao
interface ListingDao {

    @Query("SELECT * FROM cached_listings WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM cached_listings WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getAll(): List<ListingEntity>

    @Query("""
        SELECT * FROM cached_listings
        WHERE isActive = 1
          AND (title     LIKE '%' || :kw || '%'
            OR description LIKE '%' || :kw || '%')
        ORDER BY createdAt DESC
    """)
    suspend fun search(kw: String): List<ListingEntity>

    @Query("SELECT * FROM cached_listings WHERE category = :cat AND isActive = 1")
    suspend fun byCategory(cat: String): List<ListingEntity>

    @Upsert
    suspend fun upsertAll(listings: List<ListingEntity>)

    @Query("DELETE FROM cached_listings")
    suspend fun clearAll()
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(entities = [ListingEntity::class], version = 2, exportSchema = false)
abstract class UniMarketDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao

    companion object {
        @Volatile private var INSTANCE: UniMarketDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_listings ADD COLUMN condition TEXT NOT NULL DEFAULT 'Good'")
                db.execSQL("ALTER TABLE cached_listings ADD COLUMN sellerContact TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cached_listings ADD COLUMN expiresAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: android.content.Context): UniMarketDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UniMarketDatabase::class.java,
                    "unimarket_cache.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
