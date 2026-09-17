package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AutomationLog
import com.example.data.CallerRule
import com.example.data.SpamNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sub-ViewModel dedicated to caller automation rules, DTMF sequence execution,
 * spam numbers management, and automation event logging.
 */
class RulesViewModel(
    private val repository: AppRepository
) : ViewModel() {

    val allRules: StateFlow<List<CallerRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<AutomationLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spamNumbers: StateFlow<List<SpamNumber>> = repository.spamNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insertRule(rule: CallerRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRule(rule)
        }
    }

    fun updateRule(rule: CallerRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRule(rule)
        }
    }

    fun toggleRule(rule: CallerRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteRule(rule: CallerRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRule(rule)
        }
    }

    fun clearAllRules() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllRules()
        }
    }

    fun clearAutomationLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAutomationLogs()
        }
    }

    fun addSpamNumber(number: String, label: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = com.example.util.PhoneNumberNormalizer.toE164(number)
            repository.insertSpamNumber(SpamNumber(phoneNumber = number, normalizedNumber = normalized, label = label ?: "Suspected Spam"))
        }
    }

    fun removeSpamNumber(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSpamByNumber(number)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val repo = AppRepository(db.appDao())
                    return RulesViewModel(repo) as T
                }
            }
    }
}
