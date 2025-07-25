package com.example.ppg_moni.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val userId: String = UUID.randomUUID().toString(),
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val age: Int,
    val gender: Gender,
    val weight: Float, // kg
    val height: Float, // cm
    val activityLevel: ActivityLevel,
    val smokingStatus: SmokingStatus,
    val alcoholConsumption: AlcoholConsumption,
    val medicalHistory: String? = null,
    val medications: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val bmi: Float
        get() = weight / ((height / 100) * (height / 100))
    
    val isNormalWeight: Boolean
        get() = bmi in 18.5f..24.9f
}

enum class Gender {
    MALE, FEMALE, OTHER
}

enum class ActivityLevel {
    SEDENTARY,      // Ít vận động
    LIGHT,          // Vận động nhẹ  
    MODERATE,       // Vận động vừa phải
    VIGOROUS,       // Vận động mạnh
    VERY_VIGOROUS   // Vận động rất mạnh
}

enum class SmokingStatus {
    NEVER,          // Không bao giờ hút thuốc
    FORMER,         // Đã từng hút nhưng đã bỏ
    CURRENT         // Hiện tại vẫn hút
}

enum class AlcoholConsumption {
    NONE,           // Không uống
    OCCASIONAL,     // Thỉnh thoảng
    MODERATE,       // Vừa phải
    HEAVY           // Nhiều
} 