package com.sancharsaathi.app.presentation.analyzing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sancharsaathi.app.data.remote.NetworkResult
import com.sancharsaathi.app.domain.model.AnalysisRequest
import com.sancharsaathi.app.domain.model.RiskLevel
import com.sancharsaathi.app.domain.model.RiskResult
import com.sancharsaathi.app.domain.model.RiskSignal
import com.sancharsaathi.app.domain.usecase.AnalyzeContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AnalyzingUiState {
    data object Loading : AnalyzingUiState
    data class Success(val result: RiskResult) : AnalyzingUiState
    data class Error(val message: String, val retryable: Boolean) : AnalyzingUiState
}

class AnalyzingViewModel(
    private val analyzeContentUseCase: AnalyzeContentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyzingUiState>(AnalyzingUiState.Loading)
    val uiState: StateFlow<AnalyzingUiState> = _uiState.asStateFlow()

    fun analyze(request: AnalysisRequest) {
        _uiState.value = AnalyzingUiState.Loading
        viewModelScope.launch {
            when (val result = analyzeContentUseCase(request)) {
                is NetworkResult.Success -> {
                    _uiState.value = AnalyzingUiState.Success(result.data)
                }
                is NetworkResult.Failure -> {
                    _uiState.value = AnalyzingUiState.Error(
                        message = result.message,
                        retryable = true
                    )
                }
            }
        }
    }

    fun getUnverifiedFallbackResult(request: AnalysisRequest): RiskResult {
        return when (request.messageId) {
            "DEMO-LOW-001" -> RiskResult(
                analysisId = request.messageId,
                riskScore = 10,
                riskLevel = RiskLevel.LOW,
                confidence = 0.95,
                reasons = listOf(
                    "Verified DLT Sender Header (AX-INDPOST)",
                    "Official Government Domain over HTTPS (indiapost.gov.in)",
                    "No urgent or threat language detected"
                ),
                signals = listOf(
                    RiskSignal("identity", "DLT_HEADER_VALIDATED", "Sender header matches registered DLT entity.", "Header AX-INDPOST", 0.0, false),
                    RiskSignal("url", "OFFICIAL_DOMAIN", "Known safe government domain.", "indiapost.gov.in", 0.0, false)
                ),
                recommendedAction = "Message appears legitimate.",
                shouldBlock = false,
                shouldReport = false,
                detectedUrl = request.urls.firstOrNull(),
                sender = request.senderId,
                modelVersion = "1.0.0-offline",
                degraded = true,
                degradedReason = "offline_demo_fallback"
            )
            "DEMO-SUSP-002" -> RiskResult(
                analysisId = request.messageId,
                riskScore = 65,
                riskLevel = RiskLevel.SUSPICIOUS,
                confidence = 0.85,
                reasons = listOf(
                    "Unregistered Sender ID (Personal Mobile Number)",
                    "Suspicious TLD Domain (.tk)",
                    "Delivery Urgency & 24-Hour Action Window"
                ),
                signals = listOf(
                    RiskSignal("identity", "DLT_HEADER_UNREGISTERED", "Sender is an unverified mobile number.", "Sender 9876543210", 0.25, true),
                    RiskSignal("url", "SUSPICIOUS_TLD", "Domain uses high-risk free TLD (.tk).", "track-parcel-update.tk", 0.35, true)
                ),
                recommendedAction = "Exercise caution. Verify tracking status on the courier's official app.",
                shouldBlock = false,
                shouldReport = false,
                detectedUrl = request.urls.firstOrNull(),
                sender = request.senderId,
                modelVersion = "1.0.0-offline",
                degraded = true,
                degradedReason = "offline_demo_fallback"
            )
            "DEMO-HIGH-003" -> RiskResult(
                analysisId = request.messageId,
                riskScore = 85,
                riskLevel = RiskLevel.HIGH,
                confidence = 0.95,
                reasons = listOf(
                    "Brand Lookalike Impersonation (bank0findia)",
                    "DLT Sender Mismatch (Personal Mobile claiming State Bank)",
                    "Credential Harvesting Threat (Urgent PIN Request)"
                ),
                signals = listOf(
                    RiskSignal("url", "BRAND_LOOKALIKE_IMPERSONATION", "Domain impersonates State Bank of India.", "bank0findia-verify.xyz", 0.40, true),
                    RiskSignal("content", "CREDENTIAL_HARVESTING", "Requests sensitive PIN/password details.", "Urgent PIN request", 0.35, true)
                ),
                recommendedAction = "DO NOT click the link or share bank credentials. Report this phishing attempt.",
                shouldBlock = true,
                shouldReport = true,
                detectedUrl = request.urls.firstOrNull(),
                sender = request.senderId,
                modelVersion = "1.0.0-offline",
                degraded = true,
                degradedReason = "offline_demo_fallback"
            )
            else -> RiskResult(
                analysisId = "UNVERIFIED-${request.messageId}",
                riskScore = 0,
                riskLevel = RiskLevel.LOW,
                confidence = 0.0,
                reasons = listOf("Unverified — Security analysis offline."),
                signals = emptyList(),
                recommendedAction = "Verification unavailable — proceed with caution.",
                shouldBlock = false,
                shouldReport = false,
                detectedUrl = request.urls.firstOrNull(),
                sender = request.senderId,
                modelVersion = "1.0.0",
                degraded = true,
                degradedReason = "backend_unreachable"
            )
        }
    }
}
