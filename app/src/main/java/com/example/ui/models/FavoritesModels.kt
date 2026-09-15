package com.example.ui.models

import com.example.util.DeviceContact

data class PopularContactItem(
    val name: String,
    val phoneNumber: String,
    val label: String,
    val photoUri: String?,
    val callCount: Int,
    val deviceContact: DeviceContact?,
    val nickname: String? = deviceContact?.nickname
)

enum class FavCardDesign(val label: String, val styleKey: String) {
    MODERN_BENTO("Bento", "bento"),
    MATERIAL_YOU("Material", "material_you");

    companion object {
        fun fromKey(key: String): FavCardDesign {
            return entries.find { it.styleKey == key } ?: MODERN_BENTO
        }
    }
}
