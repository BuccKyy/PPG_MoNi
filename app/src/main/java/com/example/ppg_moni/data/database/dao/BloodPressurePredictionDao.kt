package com.example.ppg_moni.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.ppg_moni.data.models.BloodPressurePrediction
import com.example.ppg_moni.data.models.BPCategory
import com.example.ppg_moni.data.models.RiskLevel
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodPressurePredictionDao {
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE userId = :userId ORDER BY predictedAt DESC")
    fun getPredictionsByUser(userId: String): Flow<List<BloodPressurePrediction>>
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE userId = :userId ORDER BY predictedAt DESC LIMIT 1")
    suspend fun getLatestPrediction(userId: String): BloodPressurePrediction?
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE userId = :userId ORDER BY predictedAt DESC LIMIT 1")
    fun getLatestPredictionLiveData(userId: String): LiveData<BloodPressurePrediction?>
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE id = :id")
    suspend fun getPredictionById(id: String): BloodPressurePrediction?
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE ppgDataId = :ppgDataId")
    suspend fun getPredictionByPPGDataId(ppgDataId: String): BloodPressurePrediction?
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE userId = :userId AND category IN (:categories) ORDER BY predictedAt DESC")
    fun getPredictionsByCategory(userId: String, categories: List<BPCategory>): Flow<List<BloodPressurePrediction>>
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE userId = :userId AND riskLevel IN (:riskLevels) ORDER BY predictedAt DESC")
    fun getPredictionsByRiskLevel(userId: String, riskLevels: List<RiskLevel>): Flow<List<BloodPressurePrediction>>
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE userId = :userId AND predictedAt BETWEEN :startDate AND :endDate ORDER BY predictedAt DESC")
    fun getPredictionsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<BloodPressurePrediction>>
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE userId = :userId AND predictedAt >= :lastWeek ORDER BY predictedAt DESC")
    fun getLastWeekPredictions(userId: String, lastWeek: Long): Flow<List<BloodPressurePrediction>>
    
    @Query("SELECT * FROM blood_pressure_predictions WHERE userId = :userId AND predictedAt >= :lastMonth ORDER BY predictedAt DESC")
    fun getLastMonthPredictions(userId: String, lastMonth: Long): Flow<List<BloodPressurePrediction>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: BloodPressurePrediction): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPredictions(predictions: List<BloodPressurePrediction>): List<Long>
    
    @Update
    suspend fun updatePrediction(prediction: BloodPressurePrediction)
    
    @Delete
    suspend fun deletePrediction(prediction: BloodPressurePrediction)
    
    @Query("DELETE FROM blood_pressure_predictions WHERE id = :id")
    suspend fun deletePredictionById(id: String)
    
    @Query("DELETE FROM blood_pressure_predictions WHERE userId = :userId")
    suspend fun deleteAllPredictionsByUser(userId: String)
    
    @Query("SELECT COUNT(*) FROM blood_pressure_predictions WHERE userId = :userId")
    suspend fun getPredictionCount(userId: String): Int
    
    @Query("SELECT AVG(systolicBP) FROM blood_pressure_predictions WHERE userId = :userId")
    suspend fun getAverageSystolic(userId: String): Float?
    
    @Query("SELECT AVG(diastolicBP) FROM blood_pressure_predictions WHERE userId = :userId")
    suspend fun getAverageDiastolic(userId: String): Float?
    
    @Query("SELECT COUNT(*) FROM blood_pressure_predictions WHERE userId = :userId AND category IN (:categories)")
    suspend fun getPredictionCountByCategory(userId: String, categories: List<BPCategory>): Int
} 