package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.MainViewModel

/**
 * Central ViewModelProvider.Factory for instantiating MainViewModel and all
 * decomposed sub-ViewModels (DialerViewModel, CallLogViewModel, ContactsViewModel,
 * FavoritesViewModel, RulesViewModel).
 */
class ViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(context)
        val repository = AppRepository(db.appDao())
        val appContext = context.applicationContext

        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> {
                MainViewModel(repository, appContext) as T
            }
            modelClass.isAssignableFrom(DialerViewModel::class.java) -> {
                DialerViewModel(repository) as T
            }
            modelClass.isAssignableFrom(CallLogViewModel::class.java) -> {
                CallLogViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ContactsViewModel::class.java) -> {
                ContactsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> {
                FavoritesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(RulesViewModel::class.java) -> {
                RulesViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
