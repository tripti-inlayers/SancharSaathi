package com.sancharsaathi.app.presentation.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sancharsaathi.app.domain.model.RiskLevel
import com.sancharsaathi.app.domain.model.RiskResult
import com.sancharsaathi.app.presentation.MainActivity

class SmsNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "sanchar_saathi_alerts"
        const val CHANNEL_NAME = "Security Threat Warnings"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority warnings for detected phishing and scam SMS messages."
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showWarningNotification(result: RiskResult) {
        val title = when (result.riskLevel) {
            RiskLevel.HIGH -> "🚨 High Risk Scam SMS Detected"
            RiskLevel.SUSPICIOUS -> "⚠️ Suspicious SMS Alert"
            RiskLevel.LOW -> "SancharSaathi Security Check"
        }

        val body = result.reasons.firstOrNull() ?: "Scam or phishing patterns identified."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_ANALYSIS_ID", result.analysisId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            result.analysisId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_info_details,
                "View Threat Details",
                pendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(result.analysisId.hashCode(), notification)
    }
}
