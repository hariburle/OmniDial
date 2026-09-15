package com.example.domain.usecase

import com.example.data.AppRepository
import com.example.data.FavoriteContact
import com.example.data.IgnoredContact

/**
 * Domain use case managing favorites, card styling preferences, and popular ignore list.
 */
class ManageFavoritesUseCase(
    private val repository: AppRepository
) {
    suspend fun addFavorite(favorite: FavoriteContact) {
        repository.insertFavorite(favorite)
    }

    suspend fun removeFavorite(favorite: FavoriteContact) {
        repository.deleteFavorite(favorite)
    }

    suspend fun updateFavorite(favorite: FavoriteContact) {
        repository.updateFavorite(favorite)
    }

    suspend fun ignorePopularContact(phoneNumber: String, name: String?, category: String = "POPULAR", tag: String = "") {
        repository.insertIgnoredContact(
            IgnoredContact(
                phoneNumber = phoneNumber,
                name = name ?: phoneNumber,
                category = category,
                tag = tag,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun unignoreContact(phoneNumber: String) {
        repository.deleteIgnoredContactByNumber(phoneNumber)
    }
}
