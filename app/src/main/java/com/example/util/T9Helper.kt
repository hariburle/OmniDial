package com.example.util

data class T9SearchResult(
    val name: String,
    val phoneNumber: String,
    val label: String,
    val photoUri: String? = null,
    val nickname: String? = null,
    val matchedByName: Boolean = true,
    val matchSnippet: String = ""
)

object T9Helper {

    private val charToDigit = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9'
    )

    fun charToT9Digit(c: Char): Char? = charToDigit[c.lowercaseChar()]

    fun stringToT9(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            val digit = charToDigit[c.lowercaseChar()]
            if (digit != null) {
                sb.append(digit)
            }
        }
        return sb.toString()
    }

    /**
     * Checks if a contact name, nickname, or phone number matches the T9 digit query.
     */
    fun search(contacts: List<DeviceContact>, query: String): List<T9SearchResult> {
        val cleanQuery = query.filter { it.isDigit() }
        if (cleanQuery.isEmpty()) return emptyList()

        val results = mutableListOf<T9SearchResult>()

        for (contact in contacts) {
            // 1. Check if name matches T9
            val words = contact.name.split(Regex("\\s+"))
            var matchedName = false
            var matchSnippet = ""

            // Check full name T9
            val fullT9 = stringToT9(contact.name)
            if (fullT9.contains(cleanQuery)) {
                matchedName = true
                matchSnippet = "Name match"
            } else {
                // Check word-by-word
                for (word in words) {
                    val wordT9 = stringToT9(word)
                    if (wordT9.startsWith(cleanQuery)) {
                        matchedName = true
                        matchSnippet = "Matched '$word'"
                        break
                    }
                }
            }

            // 2. Check if nickname matches T9
            var matchedNickname = false
            if (!contact.nickname.isNullOrBlank()) {
                val nick = contact.nickname.trim()
                val nickT9 = stringToT9(nick)
                if (nickT9.contains(cleanQuery)) {
                    matchedNickname = true
                    matchSnippet = "Nickname: $nick"
                } else {
                    val nickWords = nick.split(Regex("\\s+"))
                    for (nw in nickWords) {
                        val nwT9 = stringToT9(nw)
                        if (nwT9.startsWith(cleanQuery)) {
                            matchedNickname = true
                            matchSnippet = "Nickname: $nw"
                            break
                        }
                    }
                }
            }

            // 3. Check if phone number matches digits directly (supporting saved numbers with +91, +1, etc.)
            val matchedPhone = ContactHelper.matchesNumberQuery(contact.phoneNumber, cleanQuery) ||
                contact.phoneNumbers.any { ContactHelper.matchesNumberQuery(it.number, cleanQuery) }

            if (matchedNickname) {
                results.add(
                    T9SearchResult(
                        name = contact.name,
                        phoneNumber = contact.phoneNumber,
                        label = contact.label,
                        photoUri = contact.photoUri,
                        nickname = contact.nickname,
                        matchedByName = true,
                        matchSnippet = matchSnippet
                    )
                )
            } else if (matchedName) {
                results.add(
                    T9SearchResult(
                        name = contact.name,
                        phoneNumber = contact.phoneNumber,
                        label = contact.label,
                        photoUri = contact.photoUri,
                        nickname = contact.nickname,
                        matchedByName = true,
                        matchSnippet = matchSnippet
                    )
                )
            } else if (matchedPhone) {
                // Determine matching phone number
                val matchedNumber = if (ContactHelper.matchesNumberQuery(contact.phoneNumber, cleanQuery)) {
                    contact.phoneNumber
                } else {
                    contact.phoneNumbers.firstOrNull { ContactHelper.matchesNumberQuery(it.number, cleanQuery) }?.number ?: contact.phoneNumber
                }
                results.add(
                    T9SearchResult(
                        name = contact.name,
                        phoneNumber = matchedNumber,
                        label = contact.label,
                        photoUri = contact.photoUri,
                        nickname = contact.nickname,
                        matchedByName = false,
                        matchSnippet = "Number match"
                    )
                )
            }
        }

        // Distinct by phone number and limit to top 8
        return results.distinctBy { it.phoneNumber }.take(8)
    }
}
