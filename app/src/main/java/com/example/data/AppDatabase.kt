package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ClipboardDao
import com.example.data.dao.PhoneDao
import com.example.data.dao.PhoneFieldDao
import com.example.data.entity.ClipboardItem
import com.example.data.entity.PhoneField
import com.example.data.entity.PhoneRecord

@Database(
    entities = [
        PhoneRecord::class,
        PhoneField::class,
        ClipboardItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun phoneDao(): PhoneDao
    abstract fun phoneFieldDao(): PhoneFieldDao
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "specboard_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
