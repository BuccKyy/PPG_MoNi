package com.example.ppg_moni.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.ppg_moni.data.models.PPGData
import com.example.ppg_moni.data.models.SignalQuality
import kotlinx.coroutines.flow.Flow

@Dao
interface PPGDataDao {
    
    @Query("SELECT * FROM ppg_data WHERE userId = :userId ORDER BY recordedAt DESC")
    fun getPPGDataByUser(userId: String): Flow<List<PPGData>>
    
    @Query("SELECT * FROM ppg_data WHERE userId = :userId AND isProcessed = 0 ORDER BY recordedAt DESC")
    suspend fun getUnprocessedDataByUser(userId: String): List<PPGData>
    
    @Query("SELECT * FROM ppg_data WHERE id = :id")
    suspend fun getPPGDataById(id: String): PPGData?
    
    @Query("SELECT * FROM ppg_data WHERE fileName = :fileName AND userId = :userId")
    suspend fun getPPGDataByFileName(fileName: String, userId: String): PPGData?
    
    @Query("SELECT * FROM ppg_data WHERE userId = :userId AND signalQuality IN (:qualities) ORDER BY recordedAt DESC")
    fun getPPGDataByQuality(userId: String, qualities: List<SignalQuality>): Flow<List<PPGData>>
    
    @Query("SELECT * FROM ppg_data WHERE userId = :userId AND recordedAt BETWEEN :startDate AND :endDate ORDER BY recordedAt DESC")
    fun getPPGDataByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<PPGData>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPPGData(ppgData: PPGData): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPPGDataList(ppgDataList: List<PPGData>): List<Long>
    
    @Update
    suspend fun updatePPGData(ppgData: PPGData)
    
    @Query("UPDATE ppg_data SET isProcessed = 1 WHERE id = :id")
    suspend fun markAsProcessed(id: String)
    
    @Delete
    suspend fun deletePPGData(ppgData: PPGData)
    
    @Query("DELETE FROM ppg_data WHERE id = :id")
    suspend fun deletePPGDataById(id: String)
    
    @Query("DELETE FROM ppg_data WHERE userId = :userId")
    suspend fun deleteAllPPGDataByUser(userId: String)
    
    @Query("SELECT COUNT(*) FROM ppg_data WHERE userId = :userId")
    suspend fun getPPGDataCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM ppg_data WHERE userId = :userId AND isProcessed = 0")
    suspend fun getUnprocessedDataCount(userId: String): Int
} 