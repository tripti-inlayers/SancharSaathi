package com.sancharsaathi.app

import com.sancharsaathi.app.data.local.DemoScenarioProvider
import com.sancharsaathi.app.domain.model.RiskLevel
import com.sancharsaathi.app.presentation.analyzing.AnalyzingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoScenarioTest {

    @Test
    fun testDemoScenariosProduceDistinctOfflineResults() {
        val vm = AnalyzingViewModel(
            analyzeContentUseCase = com.sancharsaathi.app.domain.usecase.AnalyzeContentUseCase(
                repository = object : com.sancharsaathi.app.data.repository.AnalysisRepository {
                    override suspend fun analyzeContent(request: com.sancharsaathi.app.domain.model.AnalysisRequest): com.sancharsaathi.app.data.remote.NetworkResult<com.sancharsaathi.app.domain.model.RiskResult> {
                        return com.sancharsaathi.app.data.remote.NetworkResult.Failure(
                            reason = com.sancharsaathi.app.data.remote.FailureReason.NO_CONNECTION,
                            message = "Offline"
                        )
                    }
                    override fun getCachedAnalysis(analysisId: String): com.sancharsaathi.app.domain.model.RiskResult? = null
                }
            )
        )

        val demo1 = DemoScenarioProvider.scenario1LowRisk()
        val demo2 = DemoScenarioProvider.scenario2Suspicious()
        val demo3 = DemoScenarioProvider.scenario3HighRisk()

        val res1 = vm.getUnverifiedFallbackResult(demo1)
        val res2 = vm.getUnverifiedFallbackResult(demo2)
        val res3 = vm.getUnverifiedFallbackResult(demo3)

        assertEquals(RiskLevel.LOW, res1.riskLevel)
        assertEquals(RiskLevel.SUSPICIOUS, res2.riskLevel)
        assertEquals(RiskLevel.HIGH, res3.riskLevel)

        assertTrue(res1.riskScore < res2.riskScore)
        assertTrue(res2.riskScore < res3.riskScore)
        assertTrue(res3.shouldBlock)
    }
}
