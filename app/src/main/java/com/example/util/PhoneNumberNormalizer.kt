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

    private val nonDigitOrPlus = Regex("[^0-9+]")
    private val nonDigitPlusStarHash = Regex("[^0-9+*#]")

    private val emergencyOrServiceCodes = setOf(
        "911", "112", "999", "000", "110", "119", "100", "101", "102", "108",
        "211", "311", "411", "511", "611", "711", "811"
    )

    private val validPhoneCharsRegex = Regex("^[+*#0-9,\\-;() .\\/]+$")
    private val extensionRegex = Regex("(?i)\\s*(ext\\.?|x)\\s*\\d+$")
    private val ussdPattern = Regex("^[*#][0-9*#]{1,15}$")

    /**
     * toE164 is a pure function of (rawNumber, defaultRegion) but costs a full libphonenumber
     * parse + validation + format. It is called from Room entity constructors, repository lookups
     * and every phone-number comparison, so the same strings are parsed thousands of times.
     */
    private const val E164_CACHE_MAX = 4096
    private val e164Cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > E164_CACHE_MAX
    }

    private fun cachedE164(key: String, compute: () -> String): String {
        synchronized(e164Cache) { e164Cache[key]?.let { return it } }
        val value = compute()
        synchronized(e164Cache) { e164Cache[key] = value }
        return value
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
        return cachedE164("$defaultRegion\u0000$rawNumber") { computeE164(rawNumber, defaultRegion) }
    }

    private fun computeE164(rawNumber: String, defaultRegion: String): String {
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
            val clean1 = s1.replace(nonDigitPlusStarHash, "")
            val clean2 = s2.replace(nonDigitPlusStarHash, "")
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
                            val clean1 = s1.replace(nonDigitOrPlus, "")
                            val clean2 = s2.replace(nonDigitOrPlus, "")
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
     * Determines whether the given text is likely to represent a phone number (or USSD/emergency/service code)
     * suitable for pasting into the dialer.
     *
     * Rejects:
     * - Null, empty, or blank strings
     * - Strings with letters or non-phone symbols (emails, URLs, words/sentences)
     * - Arbitrary numbers too short or too long (e.g. 2-digit numbers, 5-digit ZIPs, 6-digit OTPs, 16+ digit CCs)
     * - Dates or invalid digit patterns
     *
     * Accepts:
     * - International numbers: +1 650 253 0000, +91 98765 43210
     * - Domestic numbers: (650) 253-0000, 650-253-0000, 9876543210
     * - Local 7-digit numbers: 555-1234
     * - USSD / MMI / feature codes: *86, *#06#, #123#
     * - Recognized emergency and service short codes: 911, 112, 999, 611, 411
     * - Numbers with DTMF pause/wait: 1-800-555-1234,1234#
     * - tel: URIs: tel:+16502530000
     */
    fun cleanRawInput(input: CharSequence?): String {
        if (input == null) return ""
        return input.toString()
            .replace("\u200E", "") // LRM
            .replace("\u200F", "") // RLM
            .replace("\u200B", "") // Zero-width space
            .replace("\u200C", "") // ZWNJ
            .replace("\u200D", "") // ZWJ
            .replace("\uFEFF", "") // BOM
            .replace("\u202A", "") // LRE
            .replace("\u202B", "") // RLE
            .replace("\u202C", "") // PDF
            .replace("\u202D", "") // LRO
            .replace("\u202E", "") // RLO
            .replace("\u2066", "") // LRI
            .replace("\u2067", "") // RLI
            .replace("\u2068", "") // FSI
            .replace("\u2069", "") // PDI
            .replace("\u00A0", " ") // NBSP
            .replace("\u202F", " ") // Narrow NBSP
            .replace("\u2007", " ") // Figure space
            .replace("\u2009", " ") // Thin space
            .replace("\u3000", " ") // Ideographic space
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .removeSurrounding("“", "”")
            .removeSurrounding("‘", "’")
            .trim()
    }

    fun isLikelyPhoneNumber(text: CharSequence?, context: Context? = null): Boolean {
        if (text.isNullOrBlank()) return false
        val trimmed = cleanRawInput(text)
        if (trimmed.length < 3 || trimmed.length > 40) return false

        // Clean tel: scheme if present
        val clean = if (trimmed.startsWith("tel:", ignoreCase = true)) {
            trimmed.substring(4).trimStart('/', ' ')
        } else {
            trimmed
        }

        // Check for USSD / MMI codes first (e.g. *86, *#06#, #31#)
        val compactUssd = clean.replace(" ", "")
        if (ussdPattern.matches(compactUssd)) {
            val digits = compactUssd.filter { it.isDigit() }
            return digits.isNotEmpty()
        }

        // Strip trailing extension if present (e.g. "ext 102", "x12")
        val withoutExt = extensionRegex.replace(clean, "")

        // Must only contain valid phone characters (+, digits, spaces, -, ., (, ), /, ,, ;)
        if (!validPhoneCharsRegex.matches(withoutExt)) {
            return false
        }

        // Cannot have any remaining letters
        if (withoutExt.any { it in 'a'..'z' || it in 'A'..'Z' }) {
            return false
        }

        // Base portion before pause/wait delimiters
        val baseCandidate = withoutExt.takeWhile { it != ',' && it != ';' }.trim()
        val baseDigits = baseCandidate.filter { it.isDigit() }

        if (baseDigits.isEmpty()) return false

        // Check emergency and carrier short codes
        if (baseDigits in emergencyOrServiceCodes) {
            return true
        }
        @Suppress("DEPRECATION")
        try {
            if (android.telephony.PhoneNumberUtils.isEmergencyNumber(baseDigits)) {
                return true
            }
        } catch (_: Throwable) {}

        // Standard phone numbers must have between 7 and 15 digits in the base part (up to 17 if international exit code 011/00 used)
        val maxDigits = if (baseCandidate.startsWith("011") || baseCandidate.startsWith("00")) 17 else 15
        if (baseDigits.length !in 7..maxDigits) {
            return false
        }

        // Validate with Google libphonenumber if available
        val util = phoneUtil
        if (util != null) {
            val defaultRegion = getDefaultCountryIso(context)
            try {
                val parsed = util.parse(baseCandidate, defaultRegion)
                if (util.isPossibleNumber(parsed)) {
                    return true
                }
            } catch (_: Throwable) {}

            if (baseCandidate.startsWith("+")) {
                try {
                    val parsedIntl = util.parse(baseCandidate, null)
                    if (util.isPossibleNumber(parsedIntl)) {
                        return true
                    }
                } catch (_: Throwable) {}
                // Any string starting with '+' and having 7..15 digits is an international phone number
                return true
            }
        }

        // Permissive fallback: if it has 7 to maxDigits digits and only valid phone characters
        return baseDigits.length in 7..maxDigits
    }

    /**
     * Extracts a likely phone number from raw clipboard text if one is present.
     * Handles exact phone numbers, tel: URIs, or text containing a phone number.
     * Returns null if no phone number is found or text is empty/non-numeric.
     */
    fun extractLikelyPhoneNumber(text: CharSequence?, context: Context? = null): String? {
        if (text.isNullOrBlank()) return null
        val raw = cleanRawInput(text)
        if (raw.isBlank()) return null

        // 1. Direct match
        if (isLikelyPhoneNumber(raw, context)) {
            val clean = if (raw.startsWith("tel:", ignoreCase = true)) {
                raw.substring(4).trimStart('/', ' ')
            } else raw
            return clean
        }

        // 2. If text is short (<= 60 chars) and single-line, scan for embedded phone number
        if (raw.length <= 60 && !raw.contains('\n')) {
            val util = phoneUtil
            if (util != null) {
                val defaultRegion = getDefaultCountryIso(context)
                try {
                    val matches = util.findNumbers(raw, defaultRegion)
                    val firstMatch = matches.firstOrNull()
                    if (firstMatch != null) {
                        val matchedStr = firstMatch.rawString()
                        if (isLikelyPhoneNumber(matchedStr, context)) {
                            return matchedStr
                        }
                    }
                } catch (_: Throwable) {}
            }
        }

        return null
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

    /**
     * Determines whether a given phone number is international relative to the device's home country.
     */
    fun isInternational(context: Context?, rawNumber: String?): Boolean {
        if (rawNumber.isNullOrBlank()) return false
        val trimmed = rawNumber.trim()
        val defaultRegion = getDefaultCountryIso(context)
        val util = phoneUtil ?: return trimmed.startsWith("+")
        return try {
            val parsed = util.parse(trimmed, defaultRegion)
            val regionForNumber = util.getRegionCodeForNumber(parsed)
            if (!regionForNumber.isNullOrBlank()) {
                !regionForNumber.equals(defaultRegion, ignoreCase = true)
            } else {
                val defaultCountryCode = util.getCountryCodeForRegion(defaultRegion)
                parsed.countryCode != defaultCountryCode
            }
        } catch (_: Throwable) {
            trimmed.startsWith("+")
        }
    }
}
