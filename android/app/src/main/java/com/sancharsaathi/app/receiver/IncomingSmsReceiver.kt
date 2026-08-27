package com.sancharsaathi.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sancharsaathi.app.domain.capture.SmsCaptureChannel

class IncomingSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (!messages.isNullOrEmpty()) {
                val sender = messages[0].displayOriginatingAddress
                val bodyBuilder = StringBuilder()
                for (msg in messages) {
                    bodyBuilder.append(msg.displayMessageBody)
                }
                val body = bodyBuilder.toString()
                if (body.isNotBlank()) {
                    // Emit to active UI listeners if app is currently visible
                    SmsCaptureChannel.emitSms(sender, body)

                    // Safely hand off background analysis to WorkManager
                    val inputData = Data.Builder()
                        .putString(SmsBackgroundScanWorker.KEY_SENDER, sender)
                        .putString(SmsBackgroundScanWorker.KEY_BODY, body)
                        .build()

                    val workRequest = OneTimeWorkRequestBuilder<SmsBackgroundScanWorker>()
                        .setInputData(inputData)
                        .build()

                    WorkManager.getInstance(context).enqueue(workRequest)
                }
            }
        } finally {
            pendingResult.finish()
        }
    }
}
