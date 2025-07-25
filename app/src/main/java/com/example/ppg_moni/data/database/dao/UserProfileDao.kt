package com.example.ppg_moni.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.ppg_moni.data.models.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    
    @Query("SELECT * FROM user_profiles WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActiveProfiles(): Flow<List<UserProfile>>
    
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getProfileById(userId: String): UserProfile?
    
    @Query("SELECT * FROM user_profiles WHERE email = :email AND isActive = 1")
    suspend fun getProfileByEmail(email: String): UserProfile?
    
    @Query("SELECT * FROM user_profiles WHERE isActive = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getCurrentProfile(): UserProfile?
    
    @Query("SELECT * FROM user_profiles WHERE isActive = 1 ORDER BY updatedAt DESC LIMIT 1")
    fun getCurrentProfileLiveData(): LiveData<UserProfile?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile): Long
    
    @Update
    suspend fun updateProfile(profile: UserProfile)
    
    @Query("UPDATE user_profiles SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateProfile(userId: String)
    
    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()
    
    @Delete
    suspend fun deleteProfile(profile: UserProfile)
    
    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteProfileById(userId: String)
    
    @Query("SELECT COUNT(*) FROM user_profiles WHERE isActive = 1")
    suspend fun getActiveProfileCount(): Int
} 