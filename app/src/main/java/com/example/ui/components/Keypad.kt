package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalContext
import com.example.util.HapticFeedbackHelper

data class KeypadKey(val digit: Char, val subText: String = "")

val standardDialpadKeys = listOf(
    listOf(KeypadKey('1', ""), KeypadKey('2', "ABC"), KeypadKey('3', "DEF")),
    listOf(KeypadKey('4', "GHI"), KeypadKey('5', "JKL"), KeypadKey('6', "MNO")),
    listOf(KeypadKey('7', "PQRS"), KeypadKey('8', "TUV"), KeypadKey('9', "WXYZ")),
    listOf(KeypadKey('*', ""), KeypadKey('0', "+"), KeypadKey('#', ""))
)

@Composable
fun Keypad(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    speedDialMap: Map<Char, String> = emptyMap(),
    speedDialDisplayMode: String = "speed_dial_above",
    onDigitPress: (Char) -> Unit,
    onDigitRelease: (Char) -> Unit = {},
    onDigitLongPress: ((Char) -> Unit)? = null
) {
    val view = LocalView.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        standardDialpadKeys.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    KeypadButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 54.dp else 58.dp),
                        key = key,
                        compact = compact,
                        speedDialLabel = speedDialMap[key.digit],
                        speedDialDisplayMode = speedDialDisplayMode,
                        onPress = {
                            HapticFeedbackHelper.performKeypadTap(context, key.digit, view)
                            onDigitPress(key.digit)
                        },
                        onRelease = {
                            onDigitRelease(key.digit)
                        },
                        onLongPress = onDigitLongPress?.let { callback ->
                            {
                                HapticFeedbackHelper.performLongPress(context, view)
                                callback(key.digit)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun KeypadButton(
    modifier: Modifier = Modifier,
    key: KeypadKey,
    compact: Boolean,
    speedDialLabel: String? = null,
    speedDialDisplayMode: String = "speed_dial_above",
    onPress: () -> Unit,
    onRelease: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Release, is PressInteraction.Cancel -> onRelease()
                else -> {}
            }
        }
    }

    val buttonShape = RoundedCornerShape(14.dp)

    Surface(
        modifier = modifier
            .clip(buttonShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onPress,
                onLongClick = onLongPress
            )
            .testTag("keypad_digit_${key.digit}"),
        shape = buttonShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Large bold digit
            Text(
                text = key.digit.toString(),
                fontSize = if (compact) 23.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Right: Fav label on top, T9 / SubText on bottom
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                when (key.digit) {
                    '1' -> {
                        val hasFav = !speedDialLabel.isNullOrBlank()
                        val showFav = speedDialDisplayMode != "t9_only" && hasFav
                        if (showFav) {
                            Text(
                                text = speedDialLabel!!,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (speedDialDisplayMode != "speed_dial_only") {
                                Text(
                                    text = "Voicemail",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Voicemail,
                                    contentDescription = "Voicemail",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Voicemail",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                    '0' -> {
                        Text(
                            text = "Hold +",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "+",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                    '*' -> {
                        Text(
                            text = "Pause",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = ",",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                    '#' -> {
                        Text(
                            text = "Wait",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = ";",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                    else -> {
                        // Keys '2'..'9'
                        val hasFav = !speedDialLabel.isNullOrBlank()
                        val showFav = speedDialDisplayMode != "t9_only" && hasFav
                        val showT9 = speedDialDisplayMode != "speed_dial_only" && key.subText.isNotEmpty()

                        if (showFav) {
                            Text(
                                text = speedDialLabel!!,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (showT9) {
                            Text(
                                text = key.subText,
                                fontSize = if (showFav) 9.5.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        } else if (!showFav && speedDialDisplayMode == "speed_dial_only") {
                            Text(
                                text = key.subText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}
