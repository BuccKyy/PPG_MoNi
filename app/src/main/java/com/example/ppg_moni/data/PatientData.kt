package com.example.ppg_moni.data

import java.text.SimpleDateFormat
import java.util.*

data class PatientData(
    val filename: String,
    val timestamp: String,
    val redData: FloatArray,
    val irData: FloatArray,
    val systolic: Float,
    val diastolic: Float,
    val heartRate: Float,
    val oxygenSaturation: Float,
    val confidence: Float,
    val waveRatio: Float,
    val pulseWaveVelocity: Float,
    val recommendations: List<String> = emptyList()
) {
    val bloodPressureCategory: String
        get() = when {
            systolic < 120 && diastolic < 80 -> "Bình thường"
            systolic in 120f..129f && diastolic < 80 -> "Hơi cao"
            systolic in 130f..139f || diastolic in 80f..89f -> "Cao độ 1"
            systolic >= 140 || diastolic >= 90 -> "Cao độ 2"
            else -> "Không xác định"
        }
    val statusColor: String
        get() = when (bloodPressureCategory) {
            "Bình thường" -> "#4CAF50"
            "Hơi cao" -> "#FF9800"
            "Cao độ 1" -> "#FF5722"
            "Cao độ 2" -> "#F44336"
            else -> "#9E9E9E"
        }
    val explanation: String
        get() = when (bloodPressureCategory) {
            "Bình thường" -> "Tuyệt vời! Huyết áp của bạn ở mức bình thường. Hãy duy trì lối sống lành mạnh."
            "Hơi cao" -> "Huyết áp của bạn hơi cao. Nên điều chỉnh chế độ ăn uống và tập thể dục."
            "Cao độ 1" -> "Huyết áp cao độ 1. Bạn nên tham khảo ý kiến bác sĩ và thay đổi lối sống."
            "Cao độ 2" -> "Huyết áp cao độ 2. Cần gặp bác sĩ ngay để được tư vấn điều trị."
            else -> "Không thể xác định chính xác. Hãy đo lại hoặc tham khảo bác sĩ."
        }
    val waveRatioExplanation: String
        get() = when {
            waveRatio < 0.5 -> "Sóng mạch yếu - có thể do tuần hoàn kém hoặc mệt mỏi"
            waveRatio in 0.5f..0.8f -> "Sóng mạch bình thường - hệ tuần hoàn hoạt động tốt"
            waveRatio > 0.8 -> "Sóng mạch mạnh - có thể do stress hoặc hoạt động mạnh"
            else -> "Sóng mạch không xác định"
        }
    val healthRecommendations: List<String>
        get() = buildList {
            when (bloodPressureCategory) {
                "Bình thường" -> {
                    add("• Duy trì chế độ ăn ít muối")
                    add("• Tập thể dục đều đặn")
                    add("• Ngủ đủ 7-8 tiếng/ngày")
                }
                "Hơi cao" -> {
                    add("• Giảm muối trong thức ăn")
                    add("• Tăng cường ăn rau xanh")
                    add("• Đi bộ 30 phút/ngày")
                    add("• Kiểm tra định kỳ")
                }
                "Cao độ 1", "Cao độ 2" -> {
                    add("• Gặp bác sĩ để tư vấn")
                    add("• Theo dõi huyết áp hàng ngày")
                    add("• Hạn chế muối và chất béo")
                    add("• Tránh stress và căng thẳng")
                }
            }
            when {
                waveRatio < 0.5 -> add("• Nghỉ ngơi và thư giãn")
                waveRatio > 0.8 -> add("• Giảm stress, tập yoga/thiền")
            }
        }
    val readableTimestamp: String
        get() = try {
            val parts = filename.split("_")
            if (parts.size >= 3) {
                val dateStr = parts[1]
                val timeStr = parts[2].split("_")[0]
                val year = "20" + dateStr.substring(0, 2)
                val month = dateStr.substring(2, 4)
                val day = dateStr.substring(4, 6)
                val hour = timeStr.substring(0, 2)
                val minute = timeStr.substring(2, 4)
                "$day/$month/$year $hour:$minute"
            } else {
                timestamp
            }
        } catch (e: Exception) {
            timestamp
        }
} 