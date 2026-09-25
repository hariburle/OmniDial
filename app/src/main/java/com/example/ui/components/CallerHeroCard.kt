package com.example.ui.components

import android.telecom.CallAudioState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.telecom.ActiveCallInfo
import kotlin.math.abs

/**
 * Full-width "calling card" hero component for the InCallScreen active state.
 *
 * Displays the caller's high-res contact photo when available, otherwise renders
 * a name-hashed gradient background with a large monogram or person icon.
 * The caller name, phone number, and label are overlaid at the bottom with a scrim.
 */
@Composable
fun CallerHeroCard(
    callInfo: ActiveCallInfo,
    isConference: Boolean,
    isVoicemail: Boolean,
    conferenceParticipantNames: List<String> = emptyList(),
    audioRouteName: String? = null,
    audioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    modifier: Modifier = Modifier
) {
    val hasPhoto = !callInfo.photoUri.isNullOrBlank()
    val displayName = if (isConference) "Conference" else callInfo.nickname?.ifBlank { null } ?: callInfo.displayName
    val formalName = callInfo.displayName

    // Dynamic height based on audio route (desk mode vs handheld earpiece)
    val isDeskMode = audioRoute == CallAudioState.ROUTE_SPEAKER || audioRoute == CallAudioState.ROUTE_BLUETOOTH
    val targetHeight = if (isDeskMode) 280.dp else 220.dp
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hero_card_height"
    )

    // Generate a consistent gradient from the caller name hash
    val gradientColors = rememberGradientForName(displayName)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .testTag("caller_hero_card"),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp,
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background: Photo or animated gradient
            if (hasPhoto && !isConference) {
                AsyncImage(
                    model = callInfo.photoUri,
                    contentDescription = "Caller Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                )
            } else {
                // Animated gradient background
                val infiniteTransition = rememberInfiniteTransition(label = "gradient_shift")
                val animOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1000f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 8000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "gradient_offset"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = gradientColors,
                                start = Offset(animOffset, 0f),
                                end = Offset(animOffset + 600f, 800f)
                            )
                        )
                )

                // Center monogram or icon
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isVoicemail -> {
                            Icon(
                                imageVector = Icons.Default.Voicemail,
                                contentDescription = "Voicemail",
                                modifier = Modifier.size(80.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        isConference -> {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Conference call",
                                modifier = Modifier.size(80.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        else -> {
                            val rawName = callInfo.nickname?.ifBlank { null } ?: callInfo.displayName
                            val initials = getInitials(rawName)
                            if (initials.isNotBlank()) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 72.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Caller",
                                    modifier = Modifier.size(80.dp),
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom scrim overlay with caller info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Primary name
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Secondary name line (formal name if nickname is shown)
                    if (!isConference && callInfo.nickname != null &&
                        formalName.isNotBlank() &&
                        !callInfo.nickname.equals(formalName, ignoreCase = true) &&
                        formalName != callInfo.phoneNumber &&
                        formalName != "Incoming Caller"
                    ) {
                        Text(
                            text = formalName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Number / label / conference participants
                    val subtitle = if (isConference) {
                        val n = conferenceParticipantNames.size
                        if (n in 1..2) conferenceParticipantNames.joinToString(" • ")
                        else if (n > 0) "$n people"
                        else ""
                    } else {
                        buildString {
                            if (!callInfo.numberLabel.isNullOrBlank()) {
                                append(callInfo.numberLabel)
                                if (callInfo.phoneNumber.isNotBlank()) append(" • ")
                            }
                            if (callInfo.phoneNumber.isNotBlank()) {
                                append(callInfo.phoneNumber)
                            }
                        }
                    }

                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Audio route indicator (when on BT/speaker/headset)
                    if (!audioRouteName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val routeIcon = when (audioRoute) {
                            CallAudioState.ROUTE_BLUETOOTH -> Icons.Default.BluetoothAudio
                            CallAudioState.ROUTE_SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
                            CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Default.Headphones
                            else -> Icons.Default.PhoneInTalk
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = routeIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = audioRouteName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extract up to 2 initials from a name string. Filters out non-letter characters.
 */
private fun getInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { w -> w.any { it.isLetter() } }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].filter { it.isLetter() }.take(1).uppercase()
        else -> (words[0].filter { it.isLetter() }.take(1) +
                words.last().filter { it.isLetter() }.take(1)).uppercase()
    }
}

/**
 * Generate a consistent pair of gradient colors from a name hash.
 * Same name always produces the same gradient for visual continuity.
 */
@Composable
private fun rememberGradientForName(name: String): List<Color> {
    val hash = abs(name.hashCode())

    // Curated palette of elegant gradient pairs
    val palettes = listOf(
        listOf(Color(0xFF667EEA), Color(0xFF764BA2)),   // Indigo → Purple
        listOf(Color(0xFFF093FB), Color(0xFFF5576C)),   // Pink → Rose
        listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),   // Sky → Cyan
        listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),   // Green → Teal
        listOf(Color(0xFFFA709A), Color(0xFFFEE140)),   // Rose → Gold
        listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)),   // Lavender → Pink
        listOf(Color(0xFF30CFD0), Color(0xFF330867)),   // Teal → Deep Purple
        listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)),   // Salmon → Blush
        listOf(Color(0xFF667EEA), Color(0xFF43E97B)),   // Indigo → Green
        listOf(Color(0xFFFFD89B), Color(0xFF19547B)),   // Gold → Navy
    )

    return palettes[hash % palettes.size]
}
