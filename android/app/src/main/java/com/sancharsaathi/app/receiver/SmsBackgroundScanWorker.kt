package com.sancharsaathi.app.receiver

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sancharsaathi.app.data.remote.NetworkResult
import com.sancharsaathi.app.di.AppModule
import com.sancharsaathi.app.domain.model.AnalysisRequest
import com.sancharsaathi.app.domain.model.CaptureSource
import java.util.UUID

class SmsBackgroundScanWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        AppModule.ensureInitialized(appContext)

        val sender = inputData.getString(KEY_SENDER)
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()

        val urls = extractUrls(body)
        val request = AnalysisRequest(
            messageId = "SMS-${UUID.randomUUID().toString().take(8)}",
            text = body,
            urls = urls,
            senderId = sender,
            claimedOrganization = detectClaimedOrg(body),
            language = "en",
            timestampEpochMillis = System.currentTimeMillis(),
            source = CaptureSource.SMS
        )

        return try {
            when (val networkResult = AppModule.analyzeContentUseCase(request)) {
                is NetworkResult.Success -> {
                    AppModule.overlayTriggerUseCase.triggerWarningIfNeeded(networkResult.data)
                    Result.success()
                }
                is NetworkResult.Failure -> {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun extractUrls(text: String): List<String> {
        val urlRegex = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
        return urlRegex.findAll(text).map { it.value }.toList()
    }

    private fun detectClaimedOrg(text: String): String? {
        val lower = text.lowercase()
        return when {
            "sbi" in lower || "state bank" in lower -> "State Bank"
            "indiapost" in lower || "post" in lower -> "India Post"
            "irctc" in lower -> "IRCTC"
            "hdfc" in lower -> "HDFC Bank"
            "courier" in lower || "package" in lower -> "Courier Service"
            else -> null
        }
    }

    companion object {
        const val KEY_SENDER = "key_sender"
        const val KEY_BODY = "key_body"
    }
}
