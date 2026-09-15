package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.FavoriteContact
import com.example.domain.usecase.ManageFavoritesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sub-ViewModel dedicated to favorites management, speed-dial slots,
 * and drag-and-drop grid ordering.
 */
class FavoritesViewModel(
    private val repository: AppRepository,
    private val manageFavoritesUseCase: ManageFavoritesUseCase = ManageFavoritesUseCase(repository)
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteContact>> = repository.favorites
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
}
