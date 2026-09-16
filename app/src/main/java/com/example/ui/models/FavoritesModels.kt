package com.example.ui.models

import com.example.util.DeviceContact

data class PopularContactItem(
    val name: String,
    val phoneNumber: String,
    val label: String? = null,
    val photoUri: String?,
    val callCount: Int,
    val deviceContact: DeviceContact?,
    val nickname: String? = deviceContact?.nickname
)

