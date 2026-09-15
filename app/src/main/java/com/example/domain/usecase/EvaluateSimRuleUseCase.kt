package com.example.domain.usecase

import android.content.Context
import com.example.data.AppRepository
import com.example.data.CallerRule
import com.example.telecom.SimInfo

data class SimRuleEvaluationResult(
    val matchedRule: CallerRule?,
    val targetSlotIndex: Int?,
    val skipDualPrompt: Boolean,
    val explanation: String?,
    val isRoamingActive: Boolean = false,
    val isRoamingAvoided: Boolean = false,
    val ruleWeight: Int = 0
)

/**
 * Encapsulates DAG-based weighted rule conflict resolution and roaming-aware SIM selection.
 */
class EvaluateSimRuleUseCase(
    private val repository: AppRepository
) {
    suspend operator fun invoke(
        phoneNumber: String,
        sims: List<SimInfo>,
        context: Context
    ): SimRuleEvaluationResult {
        val anyRoaming = sims.any { it.isRoaming }

        if (sims.size <= 1) {
            val singleSim = sims.firstOrNull()
            return SimRuleEvaluationResult(
                matchedRule = null,
                targetSlotIndex = singleSim?.slotIndex,
                skipDualPrompt = true,
                explanation = if (singleSim?.isRoaming == true) "Single SIM (Roaming Active)" else "Single SIM device",
                isRoamingActive = singleSim?.isRoaming ?: false
            )
        }

        val rules = repository.getEnabledRules()
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

        // 1. DAG-based conflict resolution: Score and rank all matching rules
        val matchingRules = mutableListOf<ScoredRule>()

        for (rule in rules) {
            val score = calculateRuleWeight(rule, cleanNumber, phoneNumber)
            if (score != null) {
                matchingRules.add(score)
            }
        }

        // Sort descending by weight (most specific rule takes precedence)
        val bestMatch = matchingRules.maxByOrNull { it.weight }

        if (bestMatch != null) {
            val rule = bestMatch.rule
            val defaultSlot = sims.firstOrNull()?.slotIndex ?: 0
            val targetSim = sims.firstOrNull { it.slotIndex == defaultSlot }
            val alternativeSim = sims.firstOrNull { it.slotIndex != defaultSlot }

            // Roaming-aware resolution: If target SIM is roaming, prefer local non-roaming SIM
            val (resolvedSlot, roamingAvoided) = if (targetSim?.isRoaming == true && alternativeSim != null && !alternativeSim.isRoaming) {
                Pair(alternativeSim.slotIndex, true)
            } else {
                Pair(defaultSlot, false)
            }

            val roamingDesc = when {
                roamingAvoided -> " [Roaming Protected: Switched to Local SIM ${resolvedSlot + 1}]"
                targetSim?.isRoaming == true -> " [Roaming Active]"
                else -> ""
            }

            return SimRuleEvaluationResult(
                matchedRule = rule,
                targetSlotIndex = resolvedSlot,
                skipDualPrompt = true,
                explanation = "Matched Rule: ${rule.name} (${bestMatch.matchType}, Weight ${bestMatch.weight})$roamingDesc",
                isRoamingActive = anyRoaming,
                isRoamingAvoided = roamingAvoided,
                ruleWeight = bestMatch.weight
            )
        }

        // No explicit rule matched; if roaming is active on default SIM, suggest local SIM
        val defaultSim = sims.firstOrNull { it.isDefault } ?: sims.firstOrNull()
        val localNonRoamingSim = sims.firstOrNull { !it.isRoaming }
        val autoRoamingSlot = if (defaultSim?.isRoaming == true && localNonRoamingSim != null) {
            localNonRoamingSim.slotIndex
        } else {
            null
        }

        return SimRuleEvaluationResult(
            matchedRule = null,
            targetSlotIndex = autoRoamingSlot,
            skipDualPrompt = autoRoamingSlot != null,
            explanation = if (autoRoamingSlot != null) "Auto-selected non-roaming SIM ${autoRoamingSlot + 1}" else null,
            isRoamingActive = anyRoaming,
            isRoamingAvoided = autoRoamingSlot != null
        )
    }

    data class ScoredRule(val rule: CallerRule, val weight: Int, val matchType: String)

    /**
     * Calculates specificity weight for DAG rule resolution:
     * - Exact E.164 match: 1000 + length
     * - Prefix match (+1415* / ^+1415): 500 + prefix length
     * - Suffix match (*0199): 300 + suffix length
     * - Substring match: 100 + pattern length
     */
    private fun calculateRuleWeight(rule: CallerRule, cleanNumber: String, rawNumber: String): ScoredRule? {
        if (!rule.isEnabled) return null
        val pattern = rule.phoneNumberPattern.trim()
        if (pattern.isBlank()) return null

        val cleanPattern = pattern.replace(Regex("[^0-9+]"), "")

        // Exact match
        if (cleanNumber == cleanPattern || rawNumber == pattern) {
            return ScoredRule(rule, 1000 + cleanPattern.length, "Exact Match")
        }

        // Prefix match
        if (pattern.endsWith("*") || pattern.startsWith("^")) {
            val prefix = pattern.removeSuffix("*").removePrefix("^").replace(Regex("[^0-9+]"), "")
            if (cleanNumber.startsWith(prefix) || rawNumber.startsWith(prefix)) {
                return ScoredRule(rule, 500 + prefix.length, "Prefix Match")
            }
        }

        // Suffix match
        if (pattern.startsWith("*") || pattern.endsWith("$")) {
            val suffix = pattern.removePrefix("*").removeSuffix("$").replace(Regex("[^0-9+]"), "")
            if (cleanNumber.endsWith(suffix) || rawNumber.endsWith(suffix)) {
                return ScoredRule(rule, 300 + suffix.length, "Suffix Match")
            }
        }

        // Substring / wildcard match
        if (cleanNumber.contains(cleanPattern) || rawNumber.contains(pattern)) {
            return ScoredRule(rule, 100 + cleanPattern.length, "Pattern Match")
        }

        return null
    }
}
