package com.sancharsaathi.app.domain.overlay

import com.sancharsaathi.app.domain.model.RiskLevel
import com.sancharsaathi.app.domain.model.RiskResult
import com.sancharsaathi.app.presentation.overlay.SmsNotificationManager
import com.sancharsaathi.app.presentation.overlay.SmsOverlayManager

class OverlayTriggerUseCase(
    private val smsOverlayManager: SmsOverlayManager,
    private val smsNotificationManager: SmsNotificationManager
) {

    fun triggerWarningIfNeeded(result: RiskResult) {
        if (result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.SUSPICIOUS) {
            smsOverlayManager.showWarningOverlay(result)
        }
    }
}
