package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.FavoriteContact
import com.example.data.IgnoredContact
import com.example.domain.usecase.ManageFavoritesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sub-ViewModel dedicated to favorites management, speed-dial slots,
 * popular contact suggestions, and drag-and-drop grid ordering.
 */
class FavoritesViewModel(
    private val repository: AppRepository,
    private val manageFavoritesUseCase: ManageFavoritesUseCase = ManageFavoritesUseCase(repository)
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteContact>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ignoredContacts: StateFlow<List<IgnoredContact>> = repository.ignoredContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFavorite(favorite: FavoriteContact) {
        viewModelScope.launch(Dispatchers.IO) {
            manageFavoritesUseCase.addFavorite(favorite)
        }
    }

    fun removeFavorite(favorite: FavoriteContact) {
        viewModelScope.launch(Dispatchers.IO) {
            manageFavoritesUseCase.removeFavorite(favorite)
        }
    }

    fun updateFavorite(favorite: FavoriteContact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavorite(favorite)
        }
    }

    fun reorderFavorites(newOrderList: List<FavoriteContact>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavorites(newOrderList)
        }
    }

    fun assignSpeedDial(favorite: FavoriteContact, slot: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFavorite(favorite.copy(speedDialSlot = slot))
        }
    }

    fun clearSpeedDial(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val favs = repository.getAllFavoritesList()
            favs.firstOrNull { it.speedDialSlot == slot }?.let {
                repository.updateFavorite(it.copy(speedDialSlot = null))
            }
        }
    }

    fun ignoreContact(ignored: IgnoredContact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertIgnoredContact(ignored)
        }
    }

    fun unignoreContact(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteIgnoredContactByNumber(phoneNumber)
        }
    }

    fun updateIgnoredTag(phoneNumber: String, tag: String, name: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertIgnoredContact(IgnoredContact(phoneNumber = phoneNumber, name = name ?: "", tag = tag))
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val repo = AppRepository(db.appDao())
                    return FavoritesViewModel(repo) as T
                }
            }
    }
}
