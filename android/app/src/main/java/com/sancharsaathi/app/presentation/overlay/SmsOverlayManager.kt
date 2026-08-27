package com.sancharsaathi.app.presentation.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.sancharsaathi.app.domain.model.RiskLevel
import com.sancharsaathi.app.domain.model.RiskResult
import com.sancharsaathi.app.permissions.SmsPermissionManager
import com.sancharsaathi.app.presentation.MainActivity

class SmsOverlayManager(
    private val context: Context,
    private val notificationManager: SmsNotificationManager
) {

    private var activeOverlayView: View? = null

    fun showWarningOverlay(result: RiskResult) {
        if (!SmsPermissionManager.canDrawOverlays(context)) {
            notificationManager.showWarningNotification(result)
            return
        }

        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            dismissOverlay()

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 100
            }

            // Create programmatically formatted warning view
            val view = createOverlayView(result)
            activeOverlayView = view

            windowManager.addView(view, params)
        } catch (e: Exception) {
            // Fallback to heads-up notification if WindowManager layout fails
            notificationManager.showWarningNotification(result)
        }
    }

    fun dismissOverlay() {
        activeOverlayView?.let { view ->
            try {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.removeView(view)
            } catch (_: Exception) {}
            activeOverlayView = null
        }
    }

    private fun createOverlayView(result: RiskResult): View {
        val root = View(context)
        // Set view tag and click listeners for View Threat Details vs Dismiss
        val titleText = when (result.riskLevel) {
            RiskLevel.HIGH -> "🚨 SancharSaathi: High Risk Phishing Alert"
            RiskLevel.SUSPICIOUS -> "⚠️ SancharSaathi: Suspicious SMS Warning"
            RiskLevel.LOW -> "SancharSaathi Security Check"
        }
        val detailText = result.reasons.firstOrNull() ?: "Scam pattern detected in incoming message."

        // Configure click action to open MainActivity safely via Intent
        root.setOnClickListener {
            dismissOverlay()
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_ANALYSIS_ID", result.analysisId)
            }
            context.startActivity(intent)
        }

        return root
    }
}
