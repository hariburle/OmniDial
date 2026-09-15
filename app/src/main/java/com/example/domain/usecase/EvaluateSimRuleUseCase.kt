package com.example.domain.usecase

import android.content.Context
import com.example.data.AppRepository
import com.example.data.CallerRule
import com.example.telecom.SimInfo

data class SimRuleEvaluationResult(
    val matchedRule: CallerRule?,
    val targetSlotIndex: Int?,
    val skipDualPrompt: Boolean,
    val explanation: String?
)

/**
 * Encapsulates rule-based automation and SIM selection logic.
 */
class EvaluateSimRuleUseCase(
    private val repository: AppRepository
) {
    suspend operator fun invoke(
        phoneNumber: String,
        sims: List<SimInfo>,
        context: Context
    ): SimRuleEvaluationResult {
        if (sims.size <= 1) {
            return SimRuleEvaluationResult(
                matchedRule = null,
                targetSlotIndex = sims.firstOrNull()?.slotIndex,
                skipDualPrompt = true,
                explanation = "Single SIM device"
            )
        }

        val rules = repository.getEnabledRules()
        for (rule in rules) {
            if (rule.matches(phoneNumber)) {
                return SimRuleEvaluationResult(
                    matchedRule = rule,
                    targetSlotIndex = sims.firstOrNull()?.slotIndex,
                    skipDualPrompt = true,
                    explanation = "Matched Rule: ${rule.name}"
                )
            }
        }

        return SimRuleEvaluationResult(
            matchedRule = null,
            targetSlotIndex = null,
            skipDualPrompt = false,
            explanation = null
        )
    }

    private fun CallerRule.matches(number: String): Boolean {
        if (!this.isEnabled) return false
        val cleanNumber = number.replace(Regex("[^0-9+]"), "")
        val pattern = this.phoneNumberPattern.trim()
        return cleanNumber.contains(pattern) || number.contains(pattern)
    }
}
