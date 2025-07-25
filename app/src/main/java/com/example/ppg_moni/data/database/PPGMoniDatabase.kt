package com.example.ppg_moni.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ppg_moni.data.database.dao.*
import com.example.ppg_moni.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        PPGData::class,
        BloodPressurePrediction::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PPGMoniDatabase : RoomDatabase() {
    
    abstract fun userProfileDao(): UserProfileDao
    abstract fun ppgDataDao(): PPGDataDao
    abstract fun bloodPressurePredictionDao(): BloodPressurePredictionDao
    
    companion object {
        @Volatile
        private var INSTANCE: PPGMoniDatabase? = null
        
        fun getDatabase(context: Context): PPGMoniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PPGMoniDatabase::class.java,
                    "ppg_moni_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Thực hiện các tác vụ khởi tạo database nếu cần
            }
        }
    }
} 