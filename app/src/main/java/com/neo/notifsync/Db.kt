package com.neo.notifsync

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notifs", indices = [Index("time")])
data class Notif(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pkg: String,
    val app: String,
    val title: String,
    val text: String,
    val time: Long,
    val sent: Boolean = false
)

@Dao
interface NotifDao {
    @Insert suspend fun insert(n: Notif): Long
    @Query("SELECT * FROM notifs ORDER BY time DESC LIMIT 500") fun recent(): Flow<List<Notif>>
    @Query("SELECT * FROM notifs WHERE id = :id") suspend fun get(id: Long): Notif?
    @Query("UPDATE notifs SET sent = 1 WHERE id = :id") suspend fun markSent(id: Long)
    @Query("DELETE FROM notifs") suspend fun clear()
}

@Database(entities = [Notif::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): NotifDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "notifs.db")
                .build().also { inst = it }
        }
    }
}
