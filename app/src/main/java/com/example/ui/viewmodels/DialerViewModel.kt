package com.example.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.domain.usecase.SearchT9ContactsUseCase
import com.example.telecom.SimHelper
import com.example.telecom.SimInfo
import com.example.util.DeviceContact
import com.example.util.T9SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Sub-ViewModel dedicated to dialer input, DTMF keypad operations, SIM selection,
 * and instant T9 predictive contact matching.
 */
class DialerViewModel(
    private val repository: AppRepository? = null,
    private val searchT9ContactsUseCase: SearchT9ContactsUseCase = SearchT9ContactsUseCase()
) : ViewModel() {

    private val _dialerInput = MutableStateFlow("")
    val dialerInput: StateFlow<String> = _dialerInput.asStateFlow()

    private val _allContacts = MutableStateFlow<List<DeviceContact>>(emptyList())

    val matchedContacts: StateFlow<List<T9SearchResult>> = combine(_dialerInput, _allContacts) { query, contacts ->
        searchT9ContactsUseCase(query, contacts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSimSlot = MutableStateFlow(0)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    private val _availableSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val availableSims: StateFlow<List<SimInfo>> = _availableSims.asStateFlow()

    private val _selectedCallReason = MutableStateFlow<String?>(null)
    val selectedCallReason: StateFlow<String?> = _selectedCallReason.asStateFlow()

    fun updateContacts(contacts: List<DeviceContact>) {
        _allContacts.value = contacts
    }

    fun setDialerInput(text: String) {
        _dialerInput.value = text
    }

    fun appendDigit(digit: Char) {
        _dialerInput.value += digit
    }

    fun deleteLastDigit() {
        if (_dialerInput.value.isNotEmpty()) {
            _dialerInput.value = _dialerInput.value.dropLast(1)
        }
    }

    fun clearInput() {
        _dialerInput.value = ""
    }

    fun selectSimSlot(slot: Int) {
        _selectedSimSlot.value = slot
    }

    fun toggleSimSlot() {
        val sims = _availableSims.value
        if (sims.size > 1) {
            _selectedSimSlot.value = if (_selectedSimSlot.value == 0) 1 else 0
        }
    }

    fun selectCallReason(reason: String?) {
        _selectedCallReason.value = reason
    }

    fun refreshSimCards(context: Context) {
        _availableSims.value = SimHelper.getActiveSimCards(context)
    }

    fun placeCall(context: Context, number: String = _dialerInput.value) {
        val clean = number.trim()
        if (clean.isNotBlank()) {
            try {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val uri = Uri.fromParts("tel", clean, null)
                val extras = Bundle().apply {
                    putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                }
                telecomManager?.placeCall(uri, extras)
            } catch (e: Exception) {
                android.util.Log.e("DialerViewModel", "Error placing call", e)
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val repo = AppRepository(db.appDao())
                    return DialerViewModel(repo) as T
                }
            }
    }
}
