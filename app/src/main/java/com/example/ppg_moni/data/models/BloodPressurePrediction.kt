package com.example.ppg_moni.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "blood_pressure_predictions")
data class BloodPressurePrediction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val ppgDataId: String,
    val systolicBP: Float,      // mmHg
    val diastolicBP: Float,     // mmHg
    val confidence: Float,      // 0.0 - 1.0
    val category: BPCategory,
    val riskLevel: RiskLevel,
    val approximateValue: Float? = null,    // Từ ApproximateNetwork
    val refinedValue: Float? = null,        // Từ RefinementNetwork
    val heartRate: Float? = null,           // BPM
    val pulseWaveVelocity: Float? = null,   // m/s
    val predictedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val bloodPressureText: String
        get() = "${systolicBP.toInt()}/${diastolicBP.toInt()}"
    
    val isHypertensive: Boolean
        get() = category in listOf(BPCategory.STAGE_1_HYPERTENSION, BPCategory.STAGE_2_HYPERTENSION, BPCategory.HYPERTENSIVE_CRISIS)
        
    val needsImmediateAttention: Boolean
        get() = category == BPCategory.HYPERTENSIVE_CRISIS || riskLevel == RiskLevel.CRITICAL
}

enum class BPCategory(val displayName: String, val description: String, val color: String) {
    NORMAL(
        "Bình thường", 
        "Huyết áp của bạn ở mức bình thường", 
        "#4CAF50"
    ),
    ELEVATED(
        "Cao hơn bình thường", 
        "Huyết áp tâm thu cao hơn bình thường", 
        "#FF9800"
    ),
    STAGE_1_HYPERTENSION(
        "Tăng huyết áp độ 1", 
        "Bạn có dấu hiệu tăng huyết áp nhẹ", 
        "#FF5722"
    ),
    STAGE_2_HYPERTENSION(
        "Tăng huyết áp độ 2", 
        "Bạn có tăng huyết áp mức độ vừa phải", 
        "#F44336"
    ),
    HYPERTENSIVE_CRISIS(
        "Khủng hoảng tăng huyết áp", 
        "Cần chăm sóc y tế khẩn cấp ngay lập tức", 
        "#B71C1C"
    );
    
    companion object {
        fun fromBloodPressure(systolic: Float, diastolic: Float): BPCategory {
            return when {
                systolic >= 180 || diastolic >= 120 -> HYPERTENSIVE_CRISIS
                systolic >= 140 || diastolic >= 90 -> STAGE_2_HYPERTENSION
                systolic >= 130 || diastolic >= 80 -> STAGE_1_HYPERTENSION
                systolic >= 120 && diastolic < 80 -> ELEVATED
                else -> NORMAL
            }
        }
    }
}

enum class RiskLevel(val displayName: String, val description: String, val color: String) {
    LOW(
        "Thấp", 
        "Nguy cơ thấp, tiếp tục duy trì lối sống lành mạnh", 
        "#4CAF50"
    ),
    MODERATE(
        "Trung bình", 
        "Cần theo dõi và cải thiện lối sống", 
        "#FF9800"
    ),
    HIGH(
        "Cao", 
        "Nên tham khảo ý kiến bác sĩ và thay đổi lối sống", 
        "#FF5722"
    ),
    CRITICAL(
        "Rất cao", 
        "Cần tham khảo ý kiến bác sĩ ngay lập tức", 
        "#F44336"
    )
}

data class HealthRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: Int // 1-5, 5 is highest priority
)

enum class RecommendationType {
    LIFESTYLE,      // Thay đổi lối sống
    DIET,          // Chế độ ăn uống
    EXERCISE,      // Tập thể dục
    MEDICATION,    // Thuốc men
    MONITORING,    // Theo dõi
    EMERGENCY      // Khẩn cấp
} 