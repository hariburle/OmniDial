package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.RecentCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Sub-ViewModel dedicated to call history, filtering (All / Missed / Outgoing / Incoming),
 * call note persistence, reminders, and spam status updates.
 */
class CallLogViewModel(
    private val repository: AppRepository
) : ViewModel() {

    enum class CallFilter { ALL, MISSED, INCOMING, OUTGOING }

    private val _selectedFilter = MutableStateFlow(CallFilter.ALL)
    val selectedFilter: StateFlow<CallFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val rawRecentCalls: StateFlow<List<RecentCall>> = repository.recentCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRecentCalls: StateFlow<List<RecentCall>> = combine(
        rawRecentCalls,
        _selectedFilter,
        _searchQuery
    ) { calls, filter, query ->
        calls.filter { call ->
            val matchesFilter = when (filter) {
                CallFilter.ALL -> true
                CallFilter.MISSED -> call.callType == 3
                CallFilter.INCOMING -> call.callType == 1
                CallFilter.OUTGOING -> call.callType == 2
            }
            val matchesQuery = query.isBlank() ||
                call.phoneNumber.contains(query, ignoreCase = true) ||
                (call.callerName?.contains(query, ignoreCase = true) == true)

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: CallFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteRecentCall(call: RecentCall) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecentCall(call)
        }
    }

    fun deleteRecentCallById(callId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecentCallById(callId)
        }
    }

    fun updateCallNote(call: RecentCall, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRecentCall(call.copy(note = note))
        }
    }

    fun setCallReminder(call: RecentCall, reminderTimeMillis: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRecentCall(call.copy(reminderTime = reminderTimeMillis))
        }
    }
}
