package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.usecase.TrustTier

@Composable
fun TrustBadge(
    tier: TrustTier,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    if (tier == TrustTier.NEUTRAL_UNKNOWN && label.isNullOrBlank()) {
        return
    }

    val (bgColor, contentColor, icon) = when (tier) {
        TrustTier.VERIFIED_BUSINESS -> Triple(
            Color(0xFF2E7D32).copy(alpha = 0.15f),
            Color(0xFF2E7D32),
            Icons.Default.CheckCircle
        )
        TrustTier.PRIORITY_LOGISTICS -> Triple(
            Color(0xFFE65100).copy(alpha = 0.15f),
            Color(0xFFE65100),
            Icons.Default.LocalShipping
        )
        TrustTier.HIGH_RISK_SPAM -> Triple(
            Color(0xFFC62828).copy(alpha = 0.15f),
            Color(0xFFC62828),
            Icons.Default.Warning
        )
        TrustTier.NEUTRAL_UNKNOWN -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info
        )
    }

    val displayText = label ?: when (tier) {
        TrustTier.VERIFIED_BUSINESS -> "Verified Caller"
        TrustTier.PRIORITY_LOGISTICS -> "Priority Delivery"
        TrustTier.HIGH_RISK_SPAM -> "Spam Risk"
        TrustTier.NEUTRAL_UNKNOWN -> "Unverified"
    }

    Row(
        modifier = modifier
            .testTag("trust_badge_${tier.name.lowercase()}")
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = displayText,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = displayText,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
