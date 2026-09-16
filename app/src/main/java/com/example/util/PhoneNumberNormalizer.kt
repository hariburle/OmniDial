package com.example.util

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import java.util.Locale

/**
 * Enterprise-grade phone number intelligence and normalization engine powered by Google libphonenumber.
 * Handles strict E.164 parsing, carrier region inference, national format extraction,
 * and high-accuracy phone number equality comparisons.
 */
object PhoneNumberNormalizer {

    private val phoneUtil: PhoneNumberUtil? by lazy {
        try {
            PhoneNumberUtil.getInstance()
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Resolves the default country ISO code (e.g. "US", "IN", "GB") from the SIM, network, or locale.
     */
    fun getDefaultCountryIso(context: Context? = null): String {
        if (context != null) {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val simCountry = tm?.simCountryIso?.takeIf { it.isNotBlank() }
                if (simCountry != null) return simCountry.uppercase(Locale.ROOT)

                val netCountry = tm?.networkCountryIso?.takeIf { it.isNotBlank() }
                if (netCountry != null) return netCountry.uppercase(Locale.ROOT)
            } catch (_: Exception) {
                // Fall back to Locale
            }
        }
        val localeCountry = Locale.getDefault().country
        return if (!localeCountry.isNullOrBlank()) localeCountry.uppercase(Locale.ROOT) else "US"
    }

    /**
     * Normalizes any raw phone number string to canonical E.164 format (e.g. "+14155552671").
     * If the number cannot be parsed or lacks an international prefix, it uses the device default region.
     * If parsing completely fails, falls back to a clean digit representation with optional leading '+'.
     */
    fun toE164(rawNumber: String?, defaultRegion: String = "US"): String {
        if (rawNumber.isNullOrBlank()) return ""
        val trimmed = rawNumber.trim()

        val util = phoneUtil
        if (util != null) {
            try {
                val parsed: PhoneNumber = util.parse(trimmed, defaultRegion)
                if (util.isValidNumber(parsed) || util.isPossibleNumber(parsed)) {
                    return util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                }
            } catch (_: Throwable) {
                // Fall back to fallback parsing below
            }
        }

        // Clean fallback
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return if (hasPlus) "+$digits" else digits
    }

    /**
     * Normalizes a phone number to E.164 using the context to automatically detect the current country.
     */
    fun toE164WithContext(context: Context?, rawNumber: String?): String {
        val region = getDefaultCountryIso(context)
        return toE164(rawNumber, region)
    }

    /**
     * Extracts national/local digits (e.g. for +1 650 253 0000 -> "6502530000", for +91 98765 43210 -> "9876543210").
     */
    fun getNationalSignificantNumber(rawNumber: String?, defaultRegion: String = "US"): String {
        if (rawNumber.isNullOrBlank()) return ""
        val util = phoneUtil
        if (util != null) {
            try {
                val parsed: PhoneNumber = util.parse(rawNumber.trim(), defaultRegion)
                val national = util.getNationalSignificantNumber(parsed)
                if (national.isNotBlank()) return national
            } catch (_: Throwable) {
                // fallback
            }
        }
        val digits = rawNumber.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    /**
     * Formats phone number into international format (e.g. "+1 650-253-0000") or national format if local.
     */
    fun formatForDisplay(rawNumber: String?, defaultRegion: String = "US"): String {
        if (rawNumber.isNullOrBlank()) return ""
        val util = phoneUtil
        if (util != null) {
            try {
                val parsed = util.parse(rawNumber.trim(), defaultRegion)
                return util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            } catch (_: Throwable) {
                // fallback
            }
        }
        return rawNumber.trim()
    }

    /**
     * Compares two phone numbers for equality using Google libphonenumber MatchType.
     * Strictly avoids cross-country false positives (+1 vs +91).
     */
    fun isSamePhoneNumber(
        num1: String?,
        num2: String?,
        context: Context? = null,
        defaultRegion: String = getDefaultCountryIso(context)
    ): Boolean {
        if (num1.isNullOrBlank() || num2.isNullOrBlank()) return false
        val s1 = num1.trim()
        val s2 = num2.trim()
        if (s1.equals(s2, ignoreCase = true)) return true

        val d1 = s1.filter { it.isDigit() }
        val d2 = s2.filter { it.isDigit() }

        // Short codes, star codes (e.g. *86, 911, 611), or short numbers (< 7 digits)
        // must match exactly and can never fuzzy match standard phone numbers.
        if (d1.length < 7 || d2.length < 7) {
            val clean1 = s1.replace(Regex("[^0-9+*#]"), "")
            val clean2 = s2.replace(Regex("[^0-9+*#]"), "")
            return clean1.equals(clean2, ignoreCase = true)
        }

        val util = phoneUtil
        if (util != null) {
            try {
                val matchType = util.isNumberMatch(s1, s2)
                when (matchType) {
                    PhoneNumberUtil.MatchType.EXACT_MATCH,
                    PhoneNumberUtil.MatchType.NSN_MATCH -> return true
                    PhoneNumberUtil.MatchType.SHORT_NSN_MATCH -> {
                        // SHORT_NSN_MATCH only applies when both numbers have at least 7 digits,
                        // one is a suffix of the other (e.g. 7-digit local number matching 10-digit),
                        // and they do not have conflicting country codes.
                        if (d1.length >= 7 && d2.length >= 7 && (d1.endsWith(d2) || d2.endsWith(d1))) {
                            val clean1 = s1.replace(Regex("[^0-9+]"), "")
                            val clean2 = s2.replace(Regex("[^0-9+]"), "")
                            if (clean1.startsWith("+") && clean2.startsWith("+")) {
                                val e1 = toE164(clean1, defaultRegion)
                                val e2 = toE164(clean2, defaultRegion)
                                return e1 == e2
                            }
                            return true
                        }
                        return false
                    }
                    PhoneNumberUtil.MatchType.NO_MATCH -> return false
                    else -> { /* proceed to fallback */ }
                }
            } catch (_: Throwable) {
                // Fallback comparison
            }
        }

        // Fallback: compare canonical E.164
        val e1 = toE164(s1, defaultRegion)
        val e2 = toE164(s2, defaultRegion)
        if (e1.isNotBlank() && e2.isNotBlank() && e1 == e2) return true

        // Fallback: national significant numbers
        val n1 = getNationalSignificantNumber(s1, defaultRegion)
        val n2 = getNationalSignificantNumber(s2, defaultRegion)
        if (n1.isNotBlank() && n1 == n2 && n1.length >= 7) {
            val c1HasPlus = s1.trim().startsWith("+")
            val c2HasPlus = s2.trim().startsWith("+")
            if (c1HasPlus && c2HasPlus && e1 != e2) {
                return false
            }
            return true
        }

        return false
    }

    /**
     * Extracts international calling country code (e.g. 1 for US, 91 for India) if available.
     */
    fun getCountryCode(rawNumber: String?, defaultRegion: String = "US"): Int? {
        if (rawNumber.isNullOrBlank()) return null
        val util = phoneUtil ?: return null
        return try {
            val parsed = util.parse(rawNumber.trim(), defaultRegion)
            parsed.countryCode
        } catch (_: Throwable) {
            null
        }
    }
}
