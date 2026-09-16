package com.example.ui.models

enum class ContactSortBy { FIRST_NAME, LAST_NAME }
enum class ContactSortOrder { ASCENDING, DESCENDING }
enum class ContactSourceFilter { ALL, APP_ONLY, DEVICE }
enum class SmartContactSort(val label: String, val description: String) {
    ALL("All", "All Contacts (A-Z)"),
    FAVORITES("Favorites", "Favorite Contacts"),
    RECENT("Recents", "Recently Contacted"),
    FREQUENT("Frequent", "Most Frequently Contacted"),
    REDISCOVER("Rediscover", "Rediscover & Reconnect")
}
