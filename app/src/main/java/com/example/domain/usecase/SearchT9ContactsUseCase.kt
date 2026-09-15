package com.example.domain.usecase

import com.example.util.DeviceContact
import com.example.util.T9Helper
import com.example.util.T9SearchResult

/**
 * Encapsulates T9 keypad query parsing, number filtering, and rank scoring.
 */
class SearchT9ContactsUseCase {

    operator fun invoke(
        query: String,
        contacts: List<DeviceContact>
    ): List<T9SearchResult> {
        if (query.isBlank()) return emptyList()
        return T9Helper.search(contacts, query)
    }
}
