package com.example.ppg_moni.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "ppg_data")
data class PPGData(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val fileName: String,
    val signalData: FloatArray, // PPG signal data
    val samplingRate: Float = 100f, // Hz
    val duration: Float, // seconds
    val signalQuality: SignalQuality,
    val isProcessed: Boolean = false,
    val recordedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PPGData

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (fileName != other.fileName) return false
        if (!signalData.contentEquals(other.signalData)) return false
        if (samplingRate != other.samplingRate) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + signalData.contentHashCode()
        result = 31 * result + samplingRate.hashCode()
        result = 31 * result + duration.hashCode()
        return result
    }
}

enum class SignalQuality {
    EXCELLENT,      // Tín hiệu tuyệt vời
    GOOD,          // Tín hiệu tốt
    FAIR,          // Tín hiệu trung bình
    POOR,          // Tín hiệu kém
    UNUSABLE       // Không sử dụng được
}

data class ProcessedPPGSegment(
    val segmentData: FloatArray,
    val startIndex: Int,
    val endIndex: Int,
    val quality: SignalQuality
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessedPPGSegment

        if (!segmentData.contentEquals(other.segmentData)) return false
        if (startIndex != other.startIndex) return false
        if (endIndex != other.endIndex) return false
        if (quality != other.quality) return false

        return true
    }

    override fun hashCode(): Int {
        var result = segmentData.contentHashCode()
        result = 31 * result + startIndex
        result = 31 * result + endIndex
        result = 31 * result + quality.hashCode()
        return result
    }
} 