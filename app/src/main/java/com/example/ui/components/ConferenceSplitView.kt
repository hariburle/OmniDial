package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.telecom.ConferenceParticipant
import kotlin.math.abs

/**
 * Visual split-screen view for conference calls.
 *
 * For 2 participants: stacked cards showing each caller with gradient/photo.
 * For 3+: horizontal scrollable cards with an overflow indicator.
 *
 * Each card shows inline actions (end / split-to-private) so the user
 * doesn't need to open a separate dialog for basic conference management.
 */
@Composable
fun ConferenceSplitView(
    participants: List<ConferenceParticipant>,
    onEndParticipant: (String) -> Unit,
    onSplitParticipant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        participants.forEachIndexed { index, participant ->
            ConferenceParticipantCard(
                participant = participant,
                cardIndex = index,
                onEnd = { onEndParticipant(participant.id) },
                onSplit = { onSplitParticipant(participant.id) },
                showSplit = participants.size >= 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A single participant card in the conference split view.
 * Shows gradient + monogram (or photo when available via photoUri),
 * caller name, number, and inline end/private actions.
 */
@Composable
private fun ConferenceParticipantCard(
    participant: ConferenceParticipant,
    cardIndex: Int,
    onEnd: () -> Unit,
    onSplit: () -> Unit,
    showSplit: Boolean,
    modifier: Modifier = Modifier
) {
    val gradientColors = participantGradient(participant.displayName, cardIndex)

    Surface(
        modifier = modifier
            .height(100.dp)
            .testTag("conference_participant_card_${participant.id}"),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient background
            val infiniteTransition = rememberInfiniteTransition(label = "participant_gradient_$cardIndex")
            val animOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 500f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 6000 + (cardIndex * 2000), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "participant_offset_$cardIndex"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset(animOffset, 0f),
                            end = Offset(animOffset + 400f, 300f)
                        )
                    )
            )

            // Content row: avatar/monogram + name + actions
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Avatar circle + name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar circle (photo or monogram)
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        if (!participant.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = participant.photoUri,
                                contentDescription = participant.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                val initials = getParticipantInitials(participant.displayName)
                                if (initials.isNotBlank()) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 20.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Participant",
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Name + number
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = participant.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (participant.phoneNumber.isNotBlank() &&
                            participant.phoneNumber != participant.displayName
                        ) {
                            Text(
                                text = participant.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Right: Inline actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showSplit) {
                        TextButton(
                            onClick = onSplit
                        ) {
                            Text(
                                text = "Private",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onEnd,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color(0xFFFF6B6B)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End ${participant.displayName}",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Extract initials from a participant name.
 */
private fun getParticipantInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { w -> w.any { it.isLetter() } }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].filter { it.isLetter() }.take(1).uppercase()
        else -> (words[0].filter { it.isLetter() }.take(1) +
                words.last().filter { it.isLetter() }.take(1)).uppercase()
    }
}

/**
 * Generate distinct gradient colors for each conference participant.
 * Uses both name hash and card index to ensure visual differentiation.
 */
@Composable
private fun participantGradient(name: String, index: Int): List<Color> {
    val hash = abs(name.hashCode())

    // Distinct gradient pairs — offset by index so side-by-side cards are visually different
    val palettes = listOf(
        listOf(Color(0xFF667EEA), Color(0xFF764BA2)),   // Indigo → Purple
        listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),   // Sky → Cyan
        listOf(Color(0xFFFA709A), Color(0xFFFEE140)),   // Rose → Gold
        listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),   // Green → Teal
        listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)),   // Lavender → Pink
        listOf(Color(0xFF30CFD0), Color(0xFF330867)),   // Teal → Deep Purple
        listOf(Color(0xFFFFD89B), Color(0xFF19547B)),   // Gold → Navy
        listOf(Color(0xFFF093FB), Color(0xFFF5576C)),   // Pink → Rose
    )

    val paletteIndex = (hash + index * 3) % palettes.size
    return palettes[paletteIndex]
}
