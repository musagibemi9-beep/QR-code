package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.QrCodeItem

@Database(entities = [QrCodeItem::class], version = 1, exportSchema = false)
abstract class QrDatabase : RoomDatabase() {
    abstract fun qrDao(): QrDao

    companion object {
        @Volatile
        private var INSTANCE: QrDatabase? = null

        fun getDatabase(context: Context): QrDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QrDatabase::class.java,
                    "qr_studio_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
