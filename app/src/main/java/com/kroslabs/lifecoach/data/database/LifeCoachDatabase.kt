package com.kroslabs.lifecoach.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kroslabs.lifecoach.data.model.*
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        UserProfile::class,
        LifePath::class,
        Experiment::class,
        CheckIn::class,
        JournalEntry::class,
        ApiUsage::class,
        AnalyticsSnapshot::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LifeCoachDatabase : RoomDatabase() {
    abstract fun dao(): LifeCoachDao

    companion object {
        @Volatile
        private var INSTANCE: LifeCoachDatabase? = null

        fun getDatabase(context: Context, passphrase: ByteArray): LifeCoachDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeCoachDatabase::class.java,
                    "lifecoach_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
