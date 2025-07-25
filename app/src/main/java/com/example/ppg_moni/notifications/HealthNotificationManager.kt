package com.example.ppg_moni.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ppg_moni.MainActivity
import com.example.ppg_moni.R
import com.example.ppg_moni.data.models.BPCategory
import com.example.ppg_moni.data.models.BloodPressurePrediction
import com.example.ppg_moni.data.models.RiskLevel

class HealthNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID_HEALTH_ALERTS = "health_alerts"
        private const val CHANNEL_ID_REMINDERS = "reminders"
        private const val CHANNEL_ID_EMERGENCY = "emergency"
        
        private const val NOTIFICATION_ID_BP_ALERT = 1001
        private const val NOTIFICATION_ID_EMERGENCY = 1002
        private const val NOTIFICATION_ID_REMINDER = 1003
        private const val NOTIFICATION_ID_TREND_ALERT = 1004
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Health Alerts Channel
            val healthChannel = NotificationChannel(
                CHANNEL_ID_HEALTH_ALERTS,
                "Cảnh báo sức khỏe",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo về kết quả dự đoán huyết áp bất thường"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Reminders Channel
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Nhắc nhở",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nhắc nhở đo huyết áp và kiểm tra sức khỏe"
                enableVibration(false)
                setShowBadge(true)
            }
            
            // Emergency Channel
            val emergencyChannel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Khẩn cấp",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo khẩn cấp về tình trạng sức khỏe nguy hiểm"
                enableVibration(true)
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.error_color)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(listOf(
                healthChannel, reminderChannel, emergencyChannel
            ))
        }
    }
    
    /**
     * Gửi thông báo cho kết quả dự đoán huyết áp mới
     */
    fun notifyBloodPressureResult(prediction: BloodPressurePrediction) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("prediction_id", prediction.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val (channelId, title, message, priority) = when (prediction.category) {
            BPCategory.HYPERTENSIVE_CRISIS -> {
                Tuple4(
                    CHANNEL_ID_EMERGENCY,
                    "🚨 KHẨN CẤP: Huyết áp nguy hiểm!",
                    "Huyết áp ${prediction.bloodPressureText} mmHg. Cần chăm sóc y tế ngay lập tức!",
                    NotificationCompat.PRIORITY_MAX
                )
            }
            BPCategory.STAGE_2_HYPERTENSION -> {
                Tuple4(
                    CHANNEL_ID_HEALTH_ALERTS,
                    "⚠️ Huyết áp cao độ 2",
                    "Huyết áp ${prediction.bloodPressureText} mmHg. Nên tham khảo ý kiến bác sĩ.",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
            BPCategory.STAGE_1_HYPERTENSION -> {
                Tuple4(
                    CHANNEL_ID_HEALTH_ALERTS,
                    "⚠️ Huyết áp cao độ 1", 
                    "Huyết áp ${prediction.bloodPressureText} mmHg. Theo dõi và cải thiện lối sống.",
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            BPCategory.ELEVATED -> {
                Tuple4(
                    CHANNEL_ID_HEALTH_ALERTS,
                    "📈 Huyết áp cao hơn bình thường",
                    "Huyết áp ${prediction.bloodPressureText} mmHg. Cần chú ý theo dõi.",
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            BPCategory.NORMAL -> {
                Tuple4(
                    CHANNEL_ID_HEALTH_ALERTS,
                    "✅ Huyết áp bình thường",
                    "Huyết áp ${prediction.bloodPressureText} mmHg. Tiếp tục duy trì lối sống lành mạnh!",
                    NotificationCompat.PRIORITY_LOW
                )
            }
        }
        
        val color = when (prediction.category) {
            BPCategory.NORMAL -> ContextCompat.getColor(context, R.color.bp_normal_color)
            BPCategory.ELEVATED -> ContextCompat.getColor(context, R.color.bp_elevated_color)
            BPCategory.STAGE_1_HYPERTENSION -> ContextCompat.getColor(context, R.color.bp_stage1_color)
            BPCategory.STAGE_2_HYPERTENSION -> ContextCompat.getColor(context, R.color.bp_stage2_color)
            BPCategory.HYPERTENSIVE_CRISIS -> ContextCompat.getColor(context, R.color.bp_crisis_color)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_pulse)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$message\n\nĐộ tin cậy: ${(prediction.confidence * 100).toInt()}%\nMức độ nguy cơ: ${prediction.riskLevel.displayName}")
            )
            .setPriority(priority)
            .setColor(color)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BP_ALERT, notification)
        
        // Gửi emergency notification riêng nếu cần
        if (prediction.needsImmediateAttention) {
            sendEmergencyNotification(prediction)
        }
    }
    
    /**
     * Gửi thông báo khẩn cấp
     */
    private fun sendEmergencyNotification(prediction: BloodPressurePrediction) {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_emergency", true)
            putExtra("prediction_id", prediction.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Action buttons for emergency
        val callDoctorIntent = Intent(Intent.ACTION_DIAL).apply {
            data = android.net.Uri.parse("tel:115") // Emergency number in Vietnam
        }
        val callDoctorPendingIntent = PendingIntent.getActivity(
            context, 1, callDoctorIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("🚨 KHẨN CẤP: Cần chăm sóc y tế!")
            .setContentText("Huyết áp ${prediction.bloodPressureText} mmHg ở mức nguy hiểm")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Huyết áp của bạn đang ở mức nguy hiểm: ${prediction.bloodPressureText} mmHg\n\n" +
                        "Cần tìm kiếm sự chăm sóc y tế khẩn cấp ngay lập tức!\n\n" +
                        "Triệu chứng cần chú ý: đau đầu dữ dội, khó thở, đau ngực, thay đổi thị lực.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(ContextCompat.getColor(context, R.color.error_color))
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_warning, "Gọi 115", callDoctorPendingIntent)
            .addAction(R.drawable.ic_pulse, "Xem chi tiết", pendingIntent)
            .setAutoCancel(false) // Don't auto-cancel emergency notifications
            .setOngoing(true) // Make it persistent
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EMERGENCY, notification)
    }
    
    /**
     * Gửi nhắc nhở đo huyết áp
     */
    fun sendMeasurementReminder() {
        if (!hasNotificationPermission()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_measurement", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_pulse)
            .setContentTitle("⏰ Nhắc nhở đo huyết áp")
            .setContentText("Đã đến giờ đo huyết áp hàng ngày của bạn")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Việc theo dõi huyết áp thường xuyên giúp bạn:\n" +
                        "• Phát hiện sớm vấn đề sức khỏe\n" +
                        "• Quản lý tình trạng hiện tại tốt hơn\n" +
                        "• Cung cấp dữ liệu cho bác sĩ\n\n" +
                        "Chạm để đo ngay!")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(ContextCompat.getColor(context, R.color.primary_color))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_REMINDER, notification)
    }
    
    /**
     * Gửi cảnh báo về xu hướng huyết áp
     */
    fun notifyBloodPressureTrend(trendType: TrendType, recentPredictions: List<BloodPressurePrediction>) {
        if (!hasNotificationPermission() || recentPredictions.isEmpty()) return
        
        val avgSystolic = recentPredictions.map { it.systolicBP }.average().toInt()
        val avgDiastolic = recentPredictions.map { it.diastolicBP }.average().toInt()
        
        val (title, message, priority) = when (trendType) {
            TrendType.INCREASING -> {
                Triple(
                    "📈 Xu hướng huyết áp tăng",
                    "Huyết áp trung bình trong 7 ngày qua: $avgSystolic/$avgDiastolic mmHg. Cần chú ý theo dõi.",
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
            TrendType.DECREASING -> {
                Triple(
                    "📉 Xu hướng huyết áp giảm",
                    "Huyết áp trung bình trong 7 ngày qua: $avgSystolic/$avgDiastolic mmHg. Xu hướng tích cực!",
                    NotificationCompat.PRIORITY_LOW
                )
            }
            TrendType.UNSTABLE -> {
                Triple(
                    "⚠️ Huyết áp không ổn định",
                    "Huyết áp biến động nhiều trong thời gian gần đây. Nên tham khảo ý kiến bác sĩ.",
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_history", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_HEALTH_ALERTS)
            .setSmallIcon(R.drawable.ic_chart)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$message\n\nDựa trên ${recentPredictions.size} lần đo gần đây.\nXem biểu đồ để hiểu rõ hơn về xu hướng.")
            )
            .setPriority(priority)
            .setColor(ContextCompat.getColor(context, R.color.primary_color))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TREND_ALERT, notification)
    }
    
    /**
     * Hủy tất cả notifications
     */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
    
    /**
     * Hủy emergency notification
     */
    fun cancelEmergencyNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_EMERGENCY)
    }
    
    /**
     * Kiểm tra quyền thông báo
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    
    enum class TrendType {
        INCREASING,
        DECREASING, 
        UNSTABLE
    }
    
    // Helper data class for multiple values
    private data class Tuple4<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
} 