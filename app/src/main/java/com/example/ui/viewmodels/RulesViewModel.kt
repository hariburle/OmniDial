package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.AutomationLog
import com.example.data.CallerRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sub-ViewModel dedicated to caller automation rules, DTMF sequence execution,
 * and automation event logging.
 */
class RulesViewModel(
    private val repository: AppRepository
) : ViewModel() {

    val allRules: StateFlow<List<CallerRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentLogs: StateFlow<List<AutomationLog>> = repository.recentLogs
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
}
