package com.liam.scheduleu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScheduleEntity::class,
        TaskEntity::class
    ],
    version = 3,              // pastikan lebih tinggi dari versi lama
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scheduleu_db"
                )
                    // dev mode: kalau schema berubah & belum ada migration, DB direset
                    .fallbackToDestructiveMigration() // [web:358][web:282]
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
