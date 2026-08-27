import httpx
from typing import Optional, List
from app.config import settings
from app.schemas.common import RiskSignal
from app.core.logging import logger

class MlAnalysisClient:
    def __init__(self, base_url: Optional[str] = None):
        self.base_url = (base_url or settings.ML_SERVICE_URL).rstrip("/")

    async def predict(self, text: str, urls: List[str] = None) -> Optional[RiskSignal]:
        if not settings.ML_SERVICE_ENABLED:
            logger.info("ML Service is disabled in configuration.")
            return None

        url = f"{self.base_url}/predict"
        payload = {
            "text": text,
            "urls": urls or []
        }

        try:
            async with httpx.AsyncClient(timeout=settings.REQUEST_TIMEOUT_SECONDS) as client:
                response = await client.post(url, json=payload)
                if response.status_code == 200:
                    data = response.json()
                    prob = float(data.get("phishing_probability", 0.0))
                    label = data.get("predicted_label", "UNKNOWN")
                    intent = data.get("detected_intent", "GENERAL")
                    model_ver = data.get("model_version", "1.0.0")

                    if prob >= 0.70:
                        return RiskSignal(
                            category="ml_analysis",
                            code="ML_PHISHING_HIGH_CONFIDENCE",
                            description="Machine Learning model classified text as high-confidence phishing.",
                            technical_detail=f"Model v{model_ver} [{label} - Intent: {intent}] (prob={prob:.2f})",
                            weight=0.35,
                            triggered=True
                        )
                    elif prob >= 0.40:
                        return RiskSignal(
                            category="ml_analysis",
                            code="ML_SUSPICIOUS_PATTERN",
                            description="Machine Learning model detected suspicious text patterns.",
                            technical_detail=f"Model v{model_ver} [{label} - Intent: {intent}] (prob={prob:.2f})",
                            weight=0.20,
                            triggered=True
                        )
                    else:
                        return RiskSignal(
                            category="ml_analysis",
                            code="ML_SAFE_VERDICT",
                            description="Machine Learning model found low phishing probability.",
                            technical_detail=f"Model v{model_ver} [{label}] (prob={prob:.2f})",
                            weight=0.0,
                            triggered=False
                        )
                else:
                    logger.warning(f"ML Service returned non-200 status code: {response.status_code}")
                    return None
        except Exception as e:
            logger.info(f"ML service lookup unavailable or timed out: {e}")
            return None
